package org.lobid.gnd.ui.controller;

import java.util.Map;
import org.lobid.gnd.ui.config.RouterConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import reactor.core.publisher.Mono;

@Component
public final class ErrorHandler extends AbstractErrorWebExceptionHandler {

    @Bean
    @Primary
    public ErrorHandler exceptionHandler(
            ErrorAttributes errorAttributes,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer,
            ObjectProvider<ViewResolver> viewResolversProvider) {
        ErrorHandler handler =
                new ErrorHandler(errorAttributes, applicationContext, serverCodecConfigurer);
        handler.setViewResolvers(viewResolversProvider.orderedStream().toList());
        return handler;
    }

    public ErrorHandler(
            ErrorAttributes errorAttributes,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, new WebProperties.Resources(), applicationContext);
        setMessageWriters(serverCodecConfigurer.getWriters());
        setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse)
                .filter(RouterConfig.addIsDevserver());
    }

    public Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorDetails =
                getErrorAttributes(
                        request,
                        ErrorAttributeOptions.of(
                                ErrorAttributeOptions.Include.MESSAGE,
                                ErrorAttributeOptions.Include.STATUS,
                                ErrorAttributeOptions.Include.ERROR));
        int status = (int) errorDetails.getOrDefault("status", 500);
        if (request.headers().accept().contains(MediaType.TEXT_HTML)) {
            return ServerResponse.status(HttpStatus.valueOf(status))
                    .contentType(MediaType.TEXT_HTML)
                    .render(
                            "error",
                            Map.of("request", request.attributes(), "errorDetails", errorDetails));
        }
        return ServerResponse.status(HttpStatus.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorDetails));
    }

    public Mono<ServerResponse> notImplemented(ServerRequest request) {
        return errorResponse(request, 501, "Not Implemented");
    }

    static Mono<ServerResponse> errorResponse(ServerRequest request, int status, String error) {
        String message = error + ": " + request.path();
        Map<String, Object> model =
                Map.of(
                        "request",
                        request.attributes(),
                        "errorDetails",
                        Map.of("status", status, "error", error, "message", message));
        return ServerResponse.status(status).render("error", model);
    }
}
