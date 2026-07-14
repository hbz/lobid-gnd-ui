package org.lobid.gnd.ui.controller;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ApiDocHandler {

    public Mono<ServerResponse> apiDoc(ServerRequest request) {
        Map<String, Object> model =
                Map.of(
                        "request",
                        request.attributes(),
                        "baseUrl",
                        request.uri().toString().replace(request.uri().getPath(), ""));
        return ServerResponse.ok().render("api", model);
    }
}
