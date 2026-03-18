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
import reactor.util.function.Tuple2;

@Component
public class SearchHandler {

    public Mono<ServerResponse> byQ(ServerRequest request) {
        try {
            String q = request.queryParam("q").orElse("");
            return Mono.zip(
                            call("https://lobid.org/gnd/search?q=" + q),
                            call("https://lobid.org/gnd/search?format=json:suggest&q=" + q))
                    .flatMap(toResponse("search", request));
        } catch (Exception e) {
            return errorResponse(request, 500, "Search failed: " + e.getMessage());
        }
    }

    private Mono<Map<String, Object>> call(String uri) {
        return WebClient.create()
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(toJavaMap());
    }

    private Function<JsonNode, Map<String, Object>> toJavaMap() {
        return json ->
                json.isArray()
                        ? Map.of("array", new ObjectMapper().convertValue(json, Map[].class))
                        : new ObjectMapper().convertValue(json, new TypeReference<>() {});
    }

    private Function<Tuple2<Map<String, Object>, Map<String, Object>>, Mono<ServerResponse>>
            toResponse(String template, ServerRequest request) {
        return results -> {
            Map<String, Map<String, Object>> model =
                    Map.of(
                            "search",
                            results.getT1(),
                            "suggest",
                            results.getT2(),
                            "request",
                            request.attributes());
            return ServerResponse.ok().render(template, model);
        };
    }
}
