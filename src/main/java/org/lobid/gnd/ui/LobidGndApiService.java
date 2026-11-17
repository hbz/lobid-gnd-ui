package org.lobid.gnd.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.handler.timeout.TimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

@Service
public class LobidGndApiService {

    public sealed interface GndJsonResult permits GndJsonResult.JsonBody, Redirect {
        record JsonBody(JsonNode json) implements GndJsonResult {}
    }

    public sealed interface GndEntityResult permits GndEntityResult.EntityMap, Redirect {
        record EntityMap(Map<String, Object> data) implements GndEntityResult {}
    }

    public record Redirect(HttpStatusCode status, URI location)
            implements GndJsonResult, GndEntityResult {}

    @Value("${app.api}")
    private String apiBaseUrl;

    @Value("${app.dontShowOnMainPage}")
    private String[] dontShowOnMainPage;

    private static final ObjectMapper JSON = new ObjectMapper();

    public static final Cache<String, Mono<String>> labelCache =
            boundedCache(1_000_000, 1, TimeUnit.DAYS);
    public static final Cache<String, Mono<GndEntityResult>> entityCache =
            boundedCache(20_000, 1, TimeUnit.HOURS);
    public static final Cache<String, Mono<Map<String, Object>>> searchCache =
            boundedCache(20_000, 1, TimeUnit.HOURS);

    private HttpClient httpClient =
            HttpClient.create(
                            ConnectionProvider.builder("")
                                    .maxIdleTime(Duration.ofSeconds(1))
                                    .build())
                    .followRedirect(false);

    public Mono<Map<String, Object>> search(MultiValueMap<String, String> params) {
        Function<UriBuilder, URI> uriFunction =
                builder -> builder.path("/search").queryParams(params).build();
        return searchCache.get(
                uriFunction.apply(UriComponentsBuilder.newInstance()).toString(),
                k ->
                        gndCall(uriFunction)
                                .map(this::jsonBody)
                                .map(this::javaMap)
                                .cache()
                                .doOnError(e -> searchCache.invalidate(k)));
    }

    public Mono<Map<String, Object>> suggest(MultiValueMap<String, String> params) {
        return search(add(params, "format", "json:" + suggest()));
    }

    public Mono<GndEntityResult> entity(String gndId) {
        Function<UriBuilder, URI> uriFunction = builder -> builder.path("/{gndId}").build(gndId);
        return entityCache.get(
                gndId,
                key ->
                        gndCall(uriFunction)
                                .flatMap(result -> entityOrRedirect(result))
                                .cache()
                                .doOnError(e -> entityCache.invalidate(key)));
    }

    private static <T> Cache<String, Mono<T>> boundedCache(int size, int time, TimeUnit unit) {
        return Caffeine.newBuilder()
                .maximumSize(size)
                .expireAfterWrite(time, unit)
                .recordStats()
                .build();
    }

    private Mono<GndEntityResult> entityOrRedirect(GndJsonResult result) {
        return switch (result) {
            case GndJsonResult.JsonBody body ->
                    withPropertyAndTypeLabels(body.json())
                            .map(this::withImageUrlAndAttribution)
                            .map(GndEntityResult.EntityMap::new);
            case Redirect redirect -> Mono.just(redirect);
        };
    }

    public Mono<Map<String, Object>> randomEntity() {
        Function<UriBuilder, URI> uriFunction =
                builder ->
                        builder.path("/search")
                                .queryParam("q", q())
                                .queryParam("size", "1")
                                .queryParam("from", String.valueOf(new Random().nextInt(25000)))
                                .build();
        return gndCall(uriFunction)
                .map(this::jsonBody)
                .map(this::firstMemberAsMap)
                .map(this::withImageUrlAndAttribution);
    }

    private Mono<String> label(String kind, String id, String field) {
        Function<UriBuilder, URI> uriFunction =
                builder ->
                        builder.path("/reconcile/suggest/{kind}")
                                .queryParam("prefix", id)
                                .build(kind);
        return labelCache.get(
                uriFunction.apply(UriComponentsBuilder.newInstance()).toString(),
                k ->
                        gndCall(uriFunction)
                                .map(this::jsonBody)
                                .flatMap(json -> labelForId(json, kind, id, field))
                                .cache()
                                .doOnError(e -> labelCache.invalidate(k)));
    }

    private JsonNode jsonBody(GndJsonResult result) {
        return switch (result) {
            case GndJsonResult.JsonBody body -> body.json();
            case Redirect redirect ->
                    throw new IllegalStateException("Unexpected redirect: " + redirect.location());
        };
    }

    private Mono<GndJsonResult> gndCall(Function<UriBuilder, URI> uriFunction) {
        Predicate<Throwable> errorFilter =
                e -> e instanceof TimeoutException || e instanceof WebClientRequestException;
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(conf -> conf.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .baseUrl(apiBaseUrl)
                .build()
                .get()
                .uri(uriFunction)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(JsonNode.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500)).filter(errorFilter))
                .map(this::redirectOrJsonBody);
    }

    private GndJsonResult redirectOrJsonBody(ResponseEntity<JsonNode> entity) {
        return entity.getStatusCode().is3xxRedirection()
                ? new Redirect(entity.getStatusCode(), entity.getHeaders().getLocation())
                : new GndJsonResult.JsonBody(entity.getBody());
    }

    private MultiValueMap<String, String> add(
            MultiValueMap<String, String> queryParams, String key, String value) {
        MultiValueMap<String, String> newParams = new LinkedMultiValueMap<>(queryParams);
        newParams.add(key, value);
        return newParams;
    }

    private String suggest() {
        return Stream.of(
                        "preferredName",
                        "dateOfBirth-dateOfDeath",
                        "professionOrOccupation",
                        "placeOfBusiness",
                        "firstAuthor",
                        "firstComposer",
                        "dateOfProduction",
                        "geographicAreaCode")
                .collect(Collectors.joining(","));
    }

    private Map<String, Object> javaMap(JsonNode json) {
        return json.isArray()
                ? Map.of("array", JSON.convertValue(json, Map[].class))
                : JSON.convertValue(json, new TypeReference<>() {});
    }

    private String q() {
        String qParam = "depiction:*";
        for (String dont : dontShowOnMainPage) {
            qParam += " AND NOT gndIdentifier:" + dont;
        }
        return qParam;
    }

    private Map<String, Object> firstMemberAsMap(JsonNode json) {
        JsonNode firstMember = json.get("member").elements().next();
        return JSON.convertValue(firstMember, new TypeReference<>() {});
    }

    private Mono<String> labelForId(JsonNode json, String kind, String id, String field) {
        return Flux.fromIterable(() -> json.get("result").elements())
                .filter(result -> result.get("id").textValue().equals(id))
                .map(result -> result.get(field).asText())
                .defaultIfEmpty("No " + kind + " label for " + id)
                .next();
    }

    private Mono<Map<String, Object>> withPropertyAndTypeLabels(JsonNode gndEntity) {
        Stream<String> properties = asStream(gndEntity.fieldNames());
        Stream<String> types = asStream(gndEntity.get("type").elements()).map(JsonNode::asText);
        Stream<String> id = Stream.of(gndEntity.get("gndIdentifier").asText());
        return Flux.concat(
                        labelsFor(properties, "property", "name"),
                        labelsFor(types, "type", "name"),
                        labelsFor(id, "entity", "description"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                .map(labelMap -> entityWith(gndEntity, "labels", labelMap));
    }

    private Map<String, Object> entityWith(JsonNode json, String key, Map<String, String> value) {
        Map<String, Object> entity = javaMap(json);
        entity.put(key, value);
        return entity;
    }

    private <T> Stream<T> asStream(Iterator<T> fieldNames) {
        Iterable<T> iterable = () -> fieldNames;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    private Flux<Entry<String, String>> labelsFor(Stream<String> keys, String kind, String field) {
        return Flux.fromStream(keys)
                .filter(key -> !key.equals("AuthorityResource"))
                .flatMap(key -> label(kind, key, field).map(label -> Map.entry(key, label)));
    }

    private Map<String, Object> withImageUrlAndAttribution(Map<String, Object> javaMap) {
        if (javaMap.containsKey("depiction")) {
            JsonNode depictionNode = JSON.convertValue(javaMap.get("depiction"), JsonNode.class);
            String imageAttribution = createAttribution(depictionNode.get(0));
            String proxyPrefix = "https://lobid.org/imagesproxy?url=";
            javaMap.put("imageUrl", proxyPrefix + depictionNode.get(0).get("thumbnail").asText());
            javaMap.put("imageAttribution", String.format("Bildquelle: %s", imageAttribution));
        }
        return javaMap;
    }

    private String createAttribution(JsonNode depiction) {
        JsonNode license =
                Optional.ofNullable(depiction.get("license"))
                        .map(node -> node.get(0))
                        .orElse(JSON.createObjectNode());
        String artist = findText(depiction, "creatorName").replaceAll("(Unknown.*){2}", "$1");
        String licenseText = findText(license, "abbr");
        String licenseUrl = findText(license, "id");
        String fileSourceUrl = findText(depiction, "url");
        String urlForLicense = licenseUrl.isEmpty() ? fileSourceUrl : licenseUrl;
        return attributionHtml(artist, licenseText, fileSourceUrl, urlForLicense);
    }

    private String findText(JsonNode node, String field) {
        return Optional.ofNullable(node.get(field))
                .map(JsonNode::asText)
                .map(text -> text.replace("\n", " ").trim())
                .orElse("");
    }

    private String attributionHtml(
            String artist, String license, String fileSourceUrl, String licenseUrl) {
        return String.format(
                "%s%s%s",
                no(artist).orElse(String.format("%s | ", artist)),
                String.format("<a href='%s'>Wikimedia Commons</a>", fileSourceUrl),
                no(license).orElse(String.format(" | <a href='%s'>%s</a>", licenseUrl, license)));
    }

    private Optional<String> no(String string) {
        return string.isEmpty() ? Optional.of("") : Optional.empty();
    }
}
