package org.lobid.gnd.ui.controller;

import static org.lobid.gnd.ui.controller.ErrorHandler.errorResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.lobid.gnd.ui.LobidGndApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DetailsHandler {

    @Value("${app.api}")
    private String apiBaseUrl;

    private final LobidGndApiService gnd;

    public DetailsHandler(LobidGndApiService gnd) {
        this.gnd = gnd;
    }

    public Mono<ServerResponse> byId(ServerRequest request) {
        try {
            return gnd.entity(request.pathVariable("id")).flatMap(toResponse("details", request));
        } catch (Exception e) {
            return errorResponse(request, 500, "Failed to load details page: " + e.getMessage());
        }
    }

    private Function<Map<String, Object>, Mono<ServerResponse>> toResponse(
            String template, ServerRequest request) {
        return gndEntity -> {
            return Flux.fromIterable(gndEntity.keySet())
                    .flatMap(key -> gnd.label("property", key).map(label -> Map.entry(key, label)))
                    .collect(
                            () -> new HashMap<String, String>(),
                            (map, entry) -> map.put(entry.getKey(), entry.getValue()))
                    .flatMap(
                            labels -> {
                                Map<String, Object> model = new HashMap<>();
                                model.put("entity", withImageUrlAndAttribution(gndEntity));
                                model.put("labels", labels);
                                model.put("request", request.attributes());
                                return ServerResponse.ok().render(template, model);
                            });
        };
    }

    static Map<String, Object> withImageUrlAndAttribution(Map<String, Object> javaMap) {
        if (javaMap.containsKey("depiction")) {
            @SuppressWarnings("unchecked")
            var depictions = (List<Map<String, Object>>) javaMap.get("depiction");
            String imageAttribution = createAttribution(depictions.getFirst());
            String proxyPrefix = "https://lobid.org/imagesproxy?url=";
            javaMap.put("imageUrl", proxyPrefix + depictions.getFirst().get("thumbnail"));
            javaMap.put("imageAttribution", String.format("Bildquelle: %s", imageAttribution));
        }
        return javaMap;
    }

    private static String createAttribution(Map<String, Object> depiction) {
        @SuppressWarnings("unchecked")
        Map<String, Object> license =
                Optional.ofNullable(((List<Map<String, Object>>) depiction.get("license")))
                        .map(list -> list.get(0))
                        .orElse(Collections.emptyMap());
        String artist = findText(depiction, "creatorName").replaceAll("(Unknown.*){2}", "$1");
        String licenseText = findText(license, "abbr");
        String licenseUrl = findText(license, "id");
        String fileSourceUrl = findText(depiction, "url");
        String urlForLicense = licenseUrl.isEmpty() ? fileSourceUrl : licenseUrl;
        return attributionHtml(artist, licenseText, fileSourceUrl, urlForLicense);
    }

    private static String findText(Map<String, Object> map, String field) {
        Object value = map.get(field);
        value = value instanceof List ? ((List<?>) value).get(0) : value;
        return value != null ? value.toString().replace("\n", " ").trim() : "";
    }

    private static String attributionHtml(
            String artist, String license, String fileSourceUrl, String licenseUrl) {
        return String.format(
                "%s%s%s",
                no(artist).orElse(String.format("%s | ", artist)),
                String.format("<a href='%s'>Wikimedia Commons</a>", fileSourceUrl),
                no(license).orElse(String.format(" | <a href='%s'>%s</a>", licenseUrl, license)));
    }

    private static Optional<String> no(String string) {
        return string.isEmpty() ? Optional.of("") : Optional.empty();
    }
}
