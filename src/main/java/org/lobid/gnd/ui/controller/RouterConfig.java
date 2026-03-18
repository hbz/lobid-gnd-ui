package org.lobid.gnd.ui.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> detailsRoutes(
            DetailsHandler details, SearchHandler search, ErrorHandler error) {
        return RouterFunctions.route()
                .GET("/gnd", details::index)
                .GET("/gnd/search", search::byQ)
                .GET("/gnd/api", error::notImplemented)
                .GET("/gnd/dataset", error::notImplemented)
                .GET("/gnd/reconcile", error::notImplemented)
                // Define URL route for GND entry with ID, e.g. `/gnd/4031483-2`:
                .GET("/gnd/{id}", details::byId)
                .resources("/gnd/assets/**", new ClassPathResource("static/"))
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
                                .attribute("path", request.path())
                                .build());
    }
}
