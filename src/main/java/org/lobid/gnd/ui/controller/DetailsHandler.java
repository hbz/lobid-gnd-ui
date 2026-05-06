package org.lobid.gnd.ui.controller;

import static org.lobid.gnd.ui.controller.ErrorHandler.errorResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.lobid.gnd.ui.LobidGndApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class DetailsHandler {

    @Value("${app.api}")
    private String apiBaseUrl;

    private final LobidGndApiService gnd;

    public DetailsHandler(LobidGndApiService gnd) {
        this.gnd = gnd;
    }

    public Mono<ServerResponse> byId(ServerRequest request) {
        try {
            return gnd.entity(request.pathVariable("id")).flatMap(toResponse("details", request));
        } catch (Exception e) {
            return errorResponse(request, 500, "Failed to load details page: " + e.getMessage());
        }
    }

    @Component("details")
    public static class DetailsHelper {
        public String label(Map<String, String> labels, Object value) {
            return switch (value) {
                case List<?> list ->
                        list.stream().map(v -> label(labels, v)).collect(Collectors.joining(", "));
                case String s -> labels.getOrDefault(s, value.toString());
                default -> value.toString();
            };
        }
    }

    private Function<Map<String, Object>, Mono<ServerResponse>> toResponse(
            String template, ServerRequest request) {
        return gndEntity -> {
            Map<String, Map<String, Object>> model =
                    Map.of("entity", gndEntity, "request", request.attributes());
            // Render Thymeleaf template (in src/main/resources/templates) with model:
            return ServerResponse.ok().render(template, model);
        };
    }
}
