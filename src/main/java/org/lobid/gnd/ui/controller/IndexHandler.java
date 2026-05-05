package org.lobid.gnd.ui.controller;

import static org.lobid.gnd.ui.controller.ErrorHandler.errorResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;
import org.lobid.gnd.ui.LobidGndApiService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class IndexHandler {

    private final LobidGndApiService gnd;

    public IndexHandler(LobidGndApiService gnd) {
        this.gnd = gnd;
    }

    public Mono<ServerResponse> page(ServerRequest request) {
        try {
            return gnd.randomEntity().flatMap(toResponse("index", request, dataset()));
        } catch (Exception e) {
            return errorResponse(request, 500, "Failed to load index page: " + e.getMessage());
        }
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
