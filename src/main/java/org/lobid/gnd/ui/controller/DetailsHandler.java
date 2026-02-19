package org.lobid.gnd.ui.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class DetailsHandler {

    public Mono<ServerResponse> index(ServerRequest request) {
        try {
            return ServerResponse.ok()
                    .render("index", Map.of("request", request.attributes(), "dataset", dataset()));
        } catch (IOException e) {
            return response(request, 500, "Failed to load dataset file: " + e.getMessage());
        }
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

    public Mono<ServerResponse> notImplemented(ServerRequest request) {
        int statusCode = 501;
        String statusText = "Not Implemented";
        Map<String, Object> model =
                Map.of("request", request.attributes(), "status", statusCode, "error", statusText);
        return ServerResponse.status(statusCode).render("error", model);
    }

    private Map<String, Object> dataset() throws IOException {
        InputStream dataset = new ClassPathResource("static/dataset.jsonld").getInputStream();
        return new ObjectMapper().readValue(dataset, new TypeReference<>() {});
    }

    private Mono<ServerResponse> response(ServerRequest request, int status, String message) {
        Map<String, Object> model =
                Map.of("request", request.attributes(), "status", status, "error", message);
        return ServerResponse.status(status).render("error", model);
    }
}
