package org.lobid.gnd.ui.config;

import org.lobid.gnd.ui.controller.ApiCallHandler;
import org.lobid.gnd.ui.controller.ApiDocHandler;
import org.lobid.gnd.ui.controller.DatasetHandler;
import org.lobid.gnd.ui.controller.DetailsHandler;
import org.lobid.gnd.ui.controller.ErrorHandler;
import org.lobid.gnd.ui.controller.IndexHandler;
import org.lobid.gnd.ui.controller.ReconcileHandler;
import org.lobid.gnd.ui.controller.SearchHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> detailsRoutes(
            IndexHandler index,
            DetailsHandler details,
            SearchHandler search,
            ErrorHandler error,
            ApiDocHandler apiDoc,
            ReconcileHandler reconcile,
            DatasetHandler dataset,
            ApiCallHandler apiCall) {
        return RouterFunctions.route()
                .filter(apiCall.proxy())
                .route(request -> request.path().startsWith("/gnd/reconcile/"), apiCall::proxy)
                .route(request -> request.uri().getRawPath().endsWith("/"), handleTrailingSlash())
                .GET("/gnd", index::page)
                .GET("/gnd/search", search::byQ)
                .GET("/gnd/api", apiDoc::apiDoc)
                .GET("/gnd/dataset", dataset::dataset)
                .GET("/gnd/reconcile", reconcile::reconcile)
                // Define URL route for GND entry with ID, e.g. `/gnd/4031483-2`:
                .GET("/gnd/{id}", details::byId)
                .resources("/gnd/assets/**", new ClassPathResource("static/"))
                .filter(addIsDevserver())
                .build();
    }

    private HandlerFunction<ServerResponse> handleTrailingSlash() {
        return request -> {
            String oldPath = request.uri().getRawPath();
            String newPath = oldPath.substring(0, oldPath.length() - 1);
            return ServerResponse.status(HttpStatus.PERMANENT_REDIRECT)
                    .location(request.uriBuilder().replacePath(newPath).build())
                    .build();
        };
    }

    public static HandlerFilterFunction<ServerResponse, ServerResponse> addIsDevserver() {
        return (request, next) ->
                next.handle(
                        ServerRequest.from(request)
                                .attribute(
                                        "isDevserver",
                                        "1".equals(request.headers().firstHeader("X-Devserver")))
                                .attribute("path", request.path())
                                .attribute("q", request.queryParam("q").orElse(""))
                                .attribute("size", request.queryParam("size").orElse("10"))
                                .attribute("from", request.queryParam("from").orElse("0"))
                                .attribute("filter", request.queryParam("filter").orElse(""))
                                .build());
    }
}
