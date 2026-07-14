package org.lobid.gnd.ui.controller;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ReconcileHandler {

    public Mono<ServerResponse> reconcile(ServerRequest request) {
        String uri = request.uri().toString();
        Map<String, Object> model =
                Map.of(
                        "request",
                        request.attributes(),
                        "baseUrl",
                        (request.queryParams().isEmpty()
                                        ? uri
                                        : uri.substring(0, uri.lastIndexOf('?')))
                                + "/");
        return ServerResponse.ok().render("reconcile", model);
    }
}
