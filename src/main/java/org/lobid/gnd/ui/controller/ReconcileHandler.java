package org.lobid.gnd.ui.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ReconcileHandler {

    @Value("${app.reconcile}")
    private String reconcileUrl;

    public Mono<ServerResponse> reconcile(ServerRequest request) {
        Map<String, Object> model =
                Map.of("request", request.attributes(), "baseUrl", reconcileUrl);
        return ServerResponse.ok().render("reconcile", model);
    }
}
