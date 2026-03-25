package org.lobid.gnd.ui.controller;

import static org.lobid.gnd.ui.controller.ErrorHandler.errorResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
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

    @Component("icons")
    @ConfigurationProperties
    public static class IconHelper {

        private Map<String, String> icons;

        public void setIcons(Map<String, String> icons) {
            this.icons = icons;
        }

        public String iconClass(List<String> types) {
            return types.stream()
                    .filter(t -> icons.containsKey(t))
                    .map(t -> icons.get(t))
                    .findFirst()
                    .orElse("bi bi-question-circle-fill");
        }
    }

    @Component("pagination")
    static class PaginationHelper {

        public int prevFrom(String from, String size) {
            return Math.max(0, num(from) - num(size));
        }

        public int nextFrom(String total, String from, String size) {
            return Math.min(num(total), num(from) + num(size));
        }

        public String hitsFrom(String from) {
            return formatCount(num(from) + 1);
        }

        public String hitsTo(String total, String from, String size) {
            return formatCount(Math.min(num(from) + num(size), num(from) + num(total)));
        }

        public boolean disablePrev(String from) {
            return num(from) == 0;
        }

        public boolean disableNext(String total, String from, String size) {
            return num(from) + num(size) >= num(total);
        }

        public int currentPage(String from, String size) {
            return ((num(from) + 1) / num(size)) + 1;
        }

        public int lastPage(String total, String size) {
            return (num(total) % num(size) == 0)
                    ? num(total) / num(size)
                    : num(total) / num(size) + 1;
        }

        public int toPage(String currentPage, String lastPage) {
            return Math.min(Math.max(1, num(currentPage) - 4) + 9, num(lastPage));
        }

        public List<Integer> pages(String toPage) {
            int to = num(toPage);
            return IntStream.range(Math.max(1, to - 9), to).boxed().collect(Collectors.toList());
        }

        public int pageFrom(String i, String size) {
            return (num(i) * num(size)) - num(size);
        }

        public static String formatCount(int count) {
            return DecimalFormat.getInstance(Locale.GERMAN).format(count);
        }

        private int num(String value) {
            return Integer.parseInt(value);
        }
    }
}
