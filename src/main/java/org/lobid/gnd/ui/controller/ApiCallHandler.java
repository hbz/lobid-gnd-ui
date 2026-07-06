package org.lobid.gnd.ui.controller;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/** Proxy non-browser API requests directly to the lobid-gnd API. */
@Component
public class ApiCallHandler {

    private final WebClient webClient;

    public ApiCallHandler(@Value("${app.api}") String apiBaseUrl) {
        ConnectionProvider provider =
                ConnectionProvider.builder("").maxIdleTime(Duration.ofSeconds(1)).build();
        this.webClient =
                WebClient.builder()
                        .clientConnector(
                                new ReactorClientHttpConnector(HttpClient.create(provider)))
                        .codecs(conf -> conf.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .baseUrl(apiBaseUrl)
                        .build();
    }

    public HandlerFilterFunction<ServerResponse, ServerResponse> proxy() {
        return (request, next) -> {
            String apiFormats = "(json(l|ld|:.*)?|ttl|rdf|nt)";
            boolean apiCallRequest =
                    request.path().matches(".*\\." + apiFormats)
                            || request.queryParam("format").orElse("").matches(apiFormats);
            List<MediaType> acceptTypes =
                    MediaType.parseMediaTypes(request.headers().header(HttpHeaders.ACCEPT));
            boolean browserRequest =
                    request.path().endsWith("bundle.js")
                            || acceptTypes.contains(MediaType.TEXT_HTML)
                            || acceptTypes.toString().contains("text/css")
                            || acceptTypes.toString().contains("image/")
                            || acceptTypes.toString().contains("application/font");
            return browserRequest && !apiCallRequest ? next.handle(request) : proxy(request);
        };
    }

    private Mono<ServerResponse> proxy(ServerRequest request) {
        return webClient
                .get()
                .uri(uriFrom(request))
                .headers(acceptFrom(request))
                .retrieve()
                .toEntityFlux(DataBuffer.class)
                .flatMap(toResponse());
    }

    private Function<UriBuilder, URI> uriFrom(ServerRequest request) {
        return uri ->
                uri.path(request.path().replace("/gnd", ""))
                        .queryParams(request.queryParams())
                        .build();
    }

    private Consumer<HttpHeaders> acceptFrom(ServerRequest request) {
        return headers -> {
            headers.setAccept(request.headers().asHttpHeaders().getAccept());
        };
    }

    private Function<ResponseEntity<Flux<DataBuffer>>, Mono<ServerResponse>> toResponse() {
        return entity ->
                ServerResponse.status(entity.getStatusCode())
                        .contentType(entity.getHeaders().getContentType())
                        .body(entity.getBody(), DataBuffer.class);
    }
}
