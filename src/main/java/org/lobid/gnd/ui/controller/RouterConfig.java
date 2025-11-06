package org.lobid.gnd.ui.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> detailsRoutes(DetailsHandler handler) {
        return RouterFunctions.route()
                .GET("/", handler::index)
                // Define URL route for GND entry with ID, e.g. `/4031483-2`:
                .GET("/{id}", handler::byId)
                .filter(addIsDevserver())
                .build();
    }

    private HandlerFilterFunction<ServerResponse, ServerResponse> addIsDevserver() {
        return (request, next) ->
                next.handle(
                        ServerRequest.from(request)
                                .attribute(
                                        "isDevserver",
                                        "1".equals(request.headers().firstHeader("X-Devserver")))
                                .build());
    }
}
