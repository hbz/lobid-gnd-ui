package org.lobid.gnd.ui.controller;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ErrorHandler {

    public Mono<ServerResponse> notImplemented(ServerRequest request) {
        return errorResponse(request, 501, "Not Implemented");
    }

    static Mono<ServerResponse> errorResponse(ServerRequest request, int status, String message) {
        Map<String, Object> model =
                Map.of("request", request.attributes(), "status", status, "error", message);
        return ServerResponse.status(status).render("error", model);
    }
}
