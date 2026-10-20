package org.lobid.gnd.ui.controller;

import java.util.Map;
import java.util.Optional;
import org.lobid.gnd.ui.config.RouterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public final class ErrorHandler extends AbstractErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

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
        int status =
                Optional.ofNullable(errorDetails.get("status"))
                        .map(Integer.class::cast)
                        .orElse(500);
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

    static Mono<ServerResponse> apiErrorResponse(
            ServerRequest request, WebClientResponseException e) {
        log.warn(
                "[{}] {} for request {}",
                request.exchange().getRequest().getId(),
                e.getLocalizedMessage(),
                request.uri());
        int status = e.getStatusCode().value();
        Map<String, Object> model =
                Map.of(
                        "request",
                        request.attributes(),
                        "errorDetails",
                        errorDetails(request, e, status));
        return ServerResponse.status(status).render("error", model);
    }

    private static Map<String, Object> errorDetails(
            ServerRequest request, WebClientResponseException e, int status) {
        return Map.of(
                "status",
                status,
                "error",
                e.getStatusText(),
                "message",
                e.getStatusText() + (status == 404 ? ": " + request.path() : ""),
                "requestId",
                request.exchange().getRequest().getId());
    }
}
