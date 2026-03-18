package org.lobid.gnd.ui.controller;

import static org.lobid.gnd.ui.controller.ErrorHandler.errorResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.function.Function;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class SearchHandler {
    public Mono<ServerResponse> byQ(ServerRequest request) {
        try {
            return gndSearch(request.queryParam("q").orElse(""))
                    .flatMap(toResponse("search", request));
        } catch (Exception e) {
            return errorResponse(request, 500, "Search failed: " + e.getMessage());
        }
    }

    private Mono<Map<String, Object>> gndSearch(String q) {
        return WebClient.create()
                .get()
                .uri("https://lobid.org/gnd/search?q={q}", q)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> new ObjectMapper().convertValue(json, new TypeReference<>() {}));
    }

    private Function<Map<String, Object>, Mono<ServerResponse>> toResponse(
            String template, ServerRequest request) {
        return search -> {
            return ServerResponse.ok()
                    .render(template, Map.of("search", search, "request", request.attributes()));
        };
    }
}
