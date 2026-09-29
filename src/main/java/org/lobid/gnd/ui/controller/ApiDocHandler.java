package org.lobid.gnd.ui.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ApiDocHandler {

    @Value("${app.host.main}")
    private String hostUrl;

    public Mono<ServerResponse> apiDoc(ServerRequest request) {
        Map<String, Object> model = Map.of("request", request.attributes(), "baseUrl", hostUrl);
        return ServerResponse.ok().render("api", model);
    }
}
