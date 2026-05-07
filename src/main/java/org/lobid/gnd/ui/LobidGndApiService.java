package org.lobid.gnd.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class LobidGndApiService {

    @Value("${app.api}")
    private String apiBaseUrl;

    @Value("${app.dontShowOnMainPage}")
    private String[] dontShowOnMainPage;

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final ConcurrentHashMap<String, Mono<String>> cache = new ConcurrentHashMap<>();

    public Mono<Map<String, Object>> search(MultiValueMap<String, String> params) {
        Function<UriBuilder, URI> uriFunction =
                builder -> builder.path("/search").queryParams(params).build();
        return gndCall(uriFunction).map(this::javaMap);
    }

    public Mono<Map<String, Object>> suggest(MultiValueMap<String, String> params) {
        return search(add(params, "format", "json:" + suggest()));
    }

    public Mono<Map<String, Object>> entity(String gndId) {
        Function<UriBuilder, URI> uriFunction = builder -> builder.path("/{gndId}").build(gndId);
        return gndCall(uriFunction)
                .flatMap(this::withPropertyAndTypeLabels)
                .map(this::withImageUrlAndAttribution);
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
                .map(this::firstMemberAsMap)
                .map(this::withImageUrlAndAttribution);
    }

    private Mono<String> label(String kind, String id, String field) {
        Function<UriBuilder, URI> uriFunction =
                builder ->
                        builder.path("/reconcile/suggest/{kind}")
                                .queryParam("prefix", id)
                                .build(kind);
        return cache.computeIfAbsent(
                kind + ":" + id,
                key -> gndCall(uriFunction).flatMap(json -> labelForId(json, kind, id, field)));
    }

    private Mono<JsonNode> gndCall(Function<UriBuilder, URI> uriFunction) {
        return WebClient.builder()
                .codecs(conf -> conf.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .baseUrl(apiBaseUrl)
                .build()
                .get()
                .uri(uriFunction)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class);
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
