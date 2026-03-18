package org.lobid.gnd.ui.controller;

import static org.lobid.gnd.ui.controller.ErrorHandler.errorResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class SearchHandler {
    public Mono<ServerResponse> byQ(ServerRequest request) {
        try {
            String q = request.queryParam("q").orElse("");
            return errorResponse(request, 501, "Not Implemented, q=" + q);
        } catch (Exception e) {
            return errorResponse(request, 500, "Search failed: " + e.getMessage());
        }
    }
}
