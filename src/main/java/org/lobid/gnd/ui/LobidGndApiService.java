package org.lobid.gnd.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
        return gndCall(uriFunction).map(json -> javaMap(json));
    }

    public Mono<Map<String, Object>> suggest(MultiValueMap<String, String> params) {
        return search(add(params, "format", "json:" + suggest()));
    }

    public Mono<Map<String, Object>> entity(String gndId) {
        Function<UriBuilder, URI> uriFunction = builder -> builder.path("/{gndId}").build(gndId);
        return gndCall(uriFunction).map(json -> javaMap(json));
    }

    public Mono<Map<String, Object>> randomEntity() {
        Function<UriBuilder, URI> uriFunction =
                builder ->
                        builder.path("/search")
                                .queryParam("q", q())
                                .queryParam("size", "1")
                                .queryParam("from", String.valueOf(new Random().nextInt(25000)))
                                .build();
        return gndCall(uriFunction).map(json -> firstMemberAsMap(json));
    }

    public Mono<String> label(String kind, String id) {
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
}
