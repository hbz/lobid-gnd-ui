package org.lobid.gnd.ui.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class DetailsHandler {

    public Mono<ServerResponse> index(ServerRequest request) {
        try {
            return gndEntity(randomEntityWithDepiction())
                    .flatMap(toResponse("index", request, dataset()));
        } catch (IOException e) {
            return errorResponse(request, 500, "Failed to load index page: " + e.getMessage());
        }
    }

    public Mono<ServerResponse> byId(ServerRequest request) {
        try {
            return gndEntity(request.pathVariable("id"))
                    .flatMap(toResponse("details", request, dataset()));
        } catch (IOException e) {
            return errorResponse(request, 500, "Failed to load details page: " + e.getMessage());
        }
    }

    public Mono<ServerResponse> notImplemented(ServerRequest request) {
        return errorResponse(request, 501, "Not Implemented");
    }

    private Mono<Map<String, Object>> gndEntity(String gndId) {
        // Get JSON data from lobid-gnd, convert JSON data to Java Map (to be passed to template):
        return WebClient.create()
                .get()
                .uri("https://lobid.org/gnd/{id}", gndId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> new ObjectMapper().convertValue(json, new TypeReference<>() {}));
    }

    private String randomEntityWithDepiction() {
        // Temp for implementing index template, replace with actual query as in lobid-gnd:
        List<String> entities = List.of("4031483-2", "118512676", "118637649", "118548018");
        return entities.get(new Random().nextInt(entities.size()));
    }

    private Function<Map<String, Object>, Mono<ServerResponse>> toResponse(
            String template, ServerRequest request, Map<String, Object> dataset) {
        return gndEntity -> {
            Map<String, Object> entity = withImageUrlAndAttribution(gndEntity);
            Map<String, Map<String, Object>> model =
                    Map.of("entity", entity, "dataset", dataset, "request", request.attributes());
            // Render Thymeleaf template (in src/main/resources/templates) with model:
            return ServerResponse.ok().render(template, model);
        };
    }

    private Map<String, Object> dataset() throws IOException {
        InputStream dataset = new ClassPathResource("static/dataset.jsonld").getInputStream();
        return new ObjectMapper().readValue(dataset, new TypeReference<>() {});
    }

    private Mono<ServerResponse> errorResponse(ServerRequest request, int status, String message) {
        Map<String, Object> model =
                Map.of("request", request.attributes(), "status", status, "error", message);
        return ServerResponse.status(status).render("error", model);
    }

    private Map<String, Object> withImageUrlAndAttribution(Map<String, Object> javaMap) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> depictions = (List<Map<String, Object>>) javaMap.get("depiction");
        String imageAttribution = createAttribution(depictions.getFirst());
        String proxyPrefix = "https://lobid.org/imagesproxy?url=";
        javaMap.put("imageUrl", proxyPrefix + depictions.getFirst().get("thumbnail"));
        javaMap.put("imageAttribution", String.format("Bildquelle: %s", imageAttribution));
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
