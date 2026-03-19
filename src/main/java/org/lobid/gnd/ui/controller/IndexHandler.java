package org.lobid.gnd.ui.controller;

import static org.lobid.gnd.ui.controller.ErrorHandler.errorResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class IndexHandler {

    @Value("${api}")
    private String apiBaseUrl;

    public Mono<ServerResponse> page(ServerRequest request) {
        try {
            return randomGndEntity().flatMap(toResponse("index", request, dataset()));
        } catch (Exception e) {
            return errorResponse(request, 500, "Failed to load index page: " + e.getMessage());
        }
    }

    private Mono<Map<String, Object>> randomGndEntity() {
        String randomRequestUrl =
                apiBaseUrl + "/search?q=depiction:*&size=1&from=" + new Random().nextInt(25000);
        return WebClient.create()
                .get()
                .uri(randomRequestUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> firstMemberAsMap(json));
    }

    private Map<String, Object> firstMemberAsMap(JsonNode json) {
        JsonNode firstMember = json.get("member").elements().next();
        return new ObjectMapper().convertValue(firstMember, new TypeReference<>() {});
    }

    private Function<Map<String, Object>, Mono<ServerResponse>> toResponse(
            String template, ServerRequest request, Map<String, Object> dataset) {
        return gndEntity -> {
            Map<String, Object> entity = DetailsHandler.withImageUrlAndAttribution(gndEntity);
            Map<String, Map<String, Object>> model =
                    Map.of("entity", entity, "dataset", dataset, "request", request.attributes());
            return ServerResponse.ok().render(template, model);
        };
    }

    private Map<String, Object> dataset() throws IOException {
        InputStream dataset = new ClassPathResource("static/dataset.jsonld").getInputStream();
        return new ObjectMapper().readValue(dataset, new TypeReference<>() {});
    }
}
