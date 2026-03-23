package org.lobid.gnd.ui.controller;

import static org.lobid.gnd.ui.controller.ErrorHandler.errorResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Component
public class SearchHandler {

    @Value("${api}")
    private String apiBaseUrl;

    @Component("pagination")
    class PaginationHelper {
        public int prevFrom(String from, String size) {
            int prev = Integer.parseInt(from) - Integer.parseInt(size);
            return Math.max(0, prev);
        }

        public int nextFrom(String total, String from, String size) {
            int next = Integer.parseInt(from) + Integer.parseInt(size);
            return Math.min(Integer.parseInt(total), next);
        }
    }

    public Mono<ServerResponse> byQ(ServerRequest request) {
        try {
            MultiValueMap<String, String> params = request.queryParams();
            return Mono.zip(
                            searchWith(params),
                            searchWith(add(params, p -> p.add("format", "json:suggest"))))
                    .flatMap(toResponse("search", request));
        } catch (Exception e) {
            return errorResponse(request, 500, "Search failed: " + e.getMessage());
        }
    }

    private Mono<Map<String, Object>> searchWith(MultiValueMap<String, String> params) {
        return WebClient.builder()
                .codecs(conf -> conf.defaultCodecs().maxInMemorySize(512 * 1024))
                .baseUrl(apiBaseUrl)
                .build()
                .get()
                .uri(builder -> builder.path("/search").queryParams(params).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(toJavaMap());
    }

    private Function<JsonNode, Map<String, Object>> toJavaMap() {
        return json ->
                json.isArray()
                        ? Map.of("array", new ObjectMapper().convertValue(json, Map[].class))
                        : new ObjectMapper().convertValue(json, new TypeReference<>() {});
    }

    private Function<Tuple2<Map<String, Object>, Map<String, Object>>, Mono<ServerResponse>>
            toResponse(String template, ServerRequest request) {
        return results -> {
            Map<String, Map<String, Object>> model =
                    Map.of(
                            "search",
                            results.getT1(),
                            "suggest",
                            results.getT2(),
                            "request",
                            request.attributes());
            return ServerResponse.ok().render(template, model);
        };
    }

    private MultiValueMap<String, String> add(
            MultiValueMap<String, String> queryParams,
            Consumer<MultiValueMap<String, String>> object) {
        MultiValueMap<String, String> suggestQueryParams = new LinkedMultiValueMap<>(queryParams);
        object.accept(suggestQueryParams);
        return suggestQueryParams;
    }
}
