package org.lobid.gnd_ui_lobid.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class DetailsHandler {

    public Mono<ServerResponse> index(ServerRequest request) {
        return ServerResponse.ok().render("home/index", Map.of("request", request.attributes()));
    }

    public Mono<ServerResponse> byId(ServerRequest request) {

        // Get JSON data from lobid-gnd:
        Mono<JsonNode> jsonMono =
                WebClient.create()
                        .get()
                        .uri("https://lobid.org/gnd/{id}", request.pathVariable("id"))
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(JsonNode.class);

        // Convert JSON data to Java Map (to be passed to template):
        Mono<Map<String, Object>> mapMono =
                jsonMono.map(
                        json -> new ObjectMapper().convertValue(json, new TypeReference<>() {}));

        // Render Thymeleaf template details.html (in src/main/resources/templates):
        return mapMono.flatMap(
                javaMap -> {
                    Map<String, Map<String, Object>> model =
                            Map.of("entity", javaMap, "request", request.attributes());
                    return ServerResponse.ok().render("details", model);
                });
    }
}
