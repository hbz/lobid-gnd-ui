package org.lobid.gnd.ui.controller;

import io.netty.handler.logging.LogLevel;
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
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

/** Proxy non-browser API requests directly to the lobid-gnd API. */
@Component
public class ApiCallHandler {

    private final WebClient webClient;

    @Value("${app.api}")
    private String apiBaseUrl;

    public ApiCallHandler() {
        ConnectionProvider provider =
                ConnectionProvider.builder("").maxIdleTime(Duration.ofSeconds(1)).build();
        HttpClient client =
                HttpClient.create(provider)
                        .wiretap(
                                "reactor.netty.http.client.HttpClient",
                                LogLevel.INFO,
                                AdvancedByteBufFormat.TEXTUAL);
        this.webClient =
                WebClient.builder()
                        .clientConnector(new ReactorClientHttpConnector(client))
                        .codecs(conf -> conf.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build();
    }

    public HandlerFilterFunction<ServerResponse, ServerResponse> proxy() {
        return (request, next) -> {
            String apiFormats = "(json(l|ld|:.*)?|ttl|rdf|nt|preview)";
            boolean apiCallRequest =
                    pathSuffixMatchesFormat(request, apiFormats)
                            || request.queryParam("format").orElse("").matches(apiFormats);
            List<MediaType> acceptTypes =
                    MediaType.parseMediaTypes(request.headers().header(HttpHeaders.ACCEPT));
            String browserFormats = "(html|js|css|png|jpg|woff2?)";
            boolean browserRequest =
                    pathSuffixMatchesFormat(request, browserFormats)
                            || acceptTypes.contains(MediaType.TEXT_HTML)
                            || acceptTypes.toString().contains("text/css")
                            || acceptTypes.toString().contains("image/")
                            || acceptTypes.toString().contains("application/font");
            return browserRequest && !apiCallRequest ? next.handle(request) : proxy(request);
        };
    }

    private boolean pathSuffixMatchesFormat(ServerRequest request, String format) {
        return request.path().matches(".*\\." + format);
    }

    public Mono<ServerResponse> proxy(ServerRequest request) {
        return webClient
                .method(request.method())
                .uri(uriFrom(request))
                .headers(headersFrom(request))
                .body(request.bodyToFlux(DataBuffer.class), DataBuffer.class)
                .retrieve()
                .toEntityFlux(DataBuffer.class)
                .flatMap(toResponse());
    }

    private URI uriFrom(ServerRequest request) {
        String[] schemeAndRest = apiBaseUrl.split("://");
        return UriComponentsBuilder.fromUri(request.uri())
                .scheme(schemeAndRest[0])
                .host(schemeAndRest[1].split("/")[0])
                .port(-1)
                .build(true)
                .toUri();
    }

    private Consumer<HttpHeaders> headersFrom(ServerRequest request) {
        return headers -> {
            headers.setAccept(request.headers().asHttpHeaders().getAccept());
            headers.setContentType(request.headers().asHttpHeaders().getContentType());
        };
    }

    private Function<ResponseEntity<Flux<DataBuffer>>, Mono<ServerResponse>> toResponse() {
        return entity ->
                ServerResponse.status(entity.getStatusCode())
                        .headers(headers -> headers.addAll(entity.getHeaders()))
                        .body(entity.getBody(), DataBuffer.class);
    }
}
