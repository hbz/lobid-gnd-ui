package org.lobid.gnd.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    private Mono<String> label(String kind, String id) {
        Function<UriBuilder, URI> uriFunction =
                builder ->
                        builder.path("/reconcile/suggest/{kind}")
                                .queryParam("prefix", id)
                                .build(kind);
        return cache.computeIfAbsent(
                kind + ":" + id,
                key -> gndCall(uriFunction).flatMap(json -> labelForId(json, kind, id)));
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
                ? Map.of("array", new ObjectMapper().convertValue(json, Map[].class))
                : new ObjectMapper().convertValue(json, new TypeReference<>() {});
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
        return new ObjectMapper().convertValue(firstMember, new TypeReference<>() {});
    }

    private Mono<String> labelForId(JsonNode json, String kind, String id) {
        return Flux.fromIterable(() -> json.get("result").elements())
                .filter(result -> result.get("id").textValue().equals(id))
                .map(result -> result.get("name").asText())
                .defaultIfEmpty("No " + kind + " label for " + id)
                .next();
    }

    private Mono<Map<String, Object>> withPropertyAndTypeLabels(JsonNode gndEntity) {
        Stream<String> properties = toStream(gndEntity.fieldNames());
        Stream<String> types = toStream(gndEntity.get("type").elements()).map(JsonNode::asText);
        return Flux.concat(labelsFor(properties, "property"), labelsFor(types, "type"))
                .collect(
                        () -> new HashMap<String, String>(),
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()))
                .map(
                        labels -> {
                            Map<String, Object> model = javaMap(gndEntity);
                            model.put("labels", labels);
                            return model;
                        });
    }

    private <T> Stream<T> toStream(Iterator<T> fieldNames) {
        Iterable<T> iterable = () -> fieldNames;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    private Flux<Entry<String, String>> labelsFor(Stream<String> keys, String kind) {
        return Flux.fromStream(keys)
                .flatMap(key -> label(kind, key).map(label -> Map.entry(key, label)));
    }

    private Map<String, Object> withImageUrlAndAttribution(Map<String, Object> javaMap) {
        if (javaMap.containsKey("depiction")) {
            @SuppressWarnings("unchecked")
            var depictions = (List<Map<String, Object>>) javaMap.get("depiction");
            String imageAttribution = createAttribution(depictions.getFirst());
            String proxyPrefix = "https://lobid.org/imagesproxy?url=";
            javaMap.put("imageUrl", proxyPrefix + depictions.getFirst().get("thumbnail"));
            javaMap.put("imageAttribution", String.format("Bildquelle: %s", imageAttribution));
        }
        return javaMap;
    }

    private String createAttribution(Map<String, Object> depiction) {
        @SuppressWarnings("unchecked")
        Map<String, Object> license =
                Optional.ofNullable(((List<Map<String, Object>>) depiction.get("license")))
                        .map(list -> list.get(0))
                        .orElse(Collections.emptyMap());
        String artist = findText(depiction, "creatorName").replaceAll("(Unknown.*){2}", "$1");
        String licenseText = findText(license, "abbr");
        String licenseUrl = findText(license, "id");
        String fileSourceUrl = findText(depiction, "url");
        String urlForLicense = licenseUrl.isEmpty() ? fileSourceUrl : licenseUrl;
        return attributionHtml(artist, licenseText, fileSourceUrl, urlForLicense);
    }

    private String findText(Map<String, Object> map, String field) {
        Object value = map.get(field);
        value = value instanceof List ? ((List<?>) value).get(0) : value;
        return value != null ? value.toString().replace("\n", " ").trim() : "";
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
