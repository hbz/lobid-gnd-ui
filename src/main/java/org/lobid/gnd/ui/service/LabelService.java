package org.lobid.gnd.ui.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class LabelService {

    @Value("${app.api}")
    private String apiBaseUrl;

    private static final ConcurrentHashMap<String, Mono<String>> cache = new ConcurrentHashMap<>();

    public Mono<String> get(String kind, String id) {
        return cache.computeIfAbsent(kind + ":" + id, getLabel(kind, id));
    }

    private Function<String, Mono<String>> getLabel(String kind, String id) {
        return key -> {
            Function<UriBuilder, URI> uriFunction =
                    b -> b.path("/reconcile/suggest/{kind}").queryParam("prefix", id).build(kind);
            return WebClient.builder()
                    .baseUrl(apiBaseUrl)
                    .build()
                    .get()
                    .uri(uriFunction)
                    .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMap(toLabelForId(kind, id));
        };
    }

    private Function<JsonNode, Mono<String>> toLabelForId(String kind, String id) {
        return json ->
                Flux.fromIterable(() -> json.get("result").elements())
                        .filter(result -> result.get("id").textValue().equals(id))
                        .map(result -> result.get("name").asText())
                        .defaultIfEmpty("No " + kind + " label for " + id)
                        .next();
    }
}
