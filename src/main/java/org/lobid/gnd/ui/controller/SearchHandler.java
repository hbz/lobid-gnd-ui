package org.lobid.gnd.ui.controller;

import static org.lobid.gnd.ui.controller.ErrorHandler.errorResponse;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.lobid.gnd.ui.LobidGndApiService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Component
public class SearchHandler {

    private final LobidGndApiService gnd;

    public SearchHandler(LobidGndApiService gnd) {
        this.gnd = gnd;
    }

    public Mono<ServerResponse> byQ(ServerRequest request) {
        try {
            MultiValueMap<String, String> params = request.queryParams();
            return Mono.zip(gnd.search(params), gnd.suggest(params))
                    .flatMap(toResponse("search", request));
        } catch (Exception e) {
            return errorResponse(request, 500, "Search failed: " + e.getMessage());
        }
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

    @Component("facets")
    @ConfigurationProperties(prefix = "app")
    public static class FacetsHelper {

        private Map<String, String> types;

        public void setTypes(Map<String, String> types) {
            this.types = types;
        }

        public String filter(String filter, String field, String value) {
            return String.format("%s%s", filter, filterString(field, value));
        }

        public boolean isActive(String filter, String field, String value) {
            return filter.contains(filterString(field, value));
        }

        public String without(String filter, String field, String value) {
            return filter.replace(filterString(field, value), "").trim();
        }

        public Map<String, Object> findBucket(String key, List<Map<String, Object>> buckets) {
            return buckets.stream().filter(b -> b.get("key").equals(key)).findFirst().orElse(null);
        }

        public Map<String, List<Map<String, Object>>> grouped(List<Map<String, Object>> buckets) {
            return buckets.stream().collect(groupingInOrderBy(supertype()));
        }

        private Collector<Map<String, Object>, ?, Map<String, List<Map<String, Object>>>>
                groupingInOrderBy(Function<Map<String, Object>, String> supertype) {
            return Collectors.groupingBy(supertype, LinkedHashMap::new, Collectors.toList());
        }

        private Function<Map<String, Object>, String> supertype() {
            return bucket -> {
                Object key = bucket.get("key");
                return types.getOrDefault(key, key.toString());
            };
        }

        private String filterString(String field, String value) {
            value = value.startsWith("http") ? String.format("\"%s\"", value) : value;
            return String.format("+(%s:%s)", field, value);
        }
    }

    @Component("icons")
    @ConfigurationProperties(prefix = "app")
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
            return formatCount(Math.min(num(from) + num(size), num(total)));
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
            return IntStream.range(Math.max(1, to - 9), to + 1)
                    .boxed()
                    .collect(Collectors.toList());
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
