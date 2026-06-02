package org.lobid.gnd.ui.controller;

import static org.lobid.gnd.ui.controller.ErrorHandler.errorResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.lobid.gnd.ui.LobidGndApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class DetailsHandler {

    @Value("${app.api}")
    private String apiBaseUrl;

    @Value("${app.fieldOrder}")
    private String[] fieldOrder;

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

    @Component("details")
    public static class DetailsHelper {
        public String label(Map<String, String> labels, Object value) {
            return switch (value) {
                case List<?> list ->
                        list.stream().map(v -> label(labels, v)).collect(Collectors.joining(", "));
                case String s -> labels.getOrDefault(s, value.toString());
                default -> value.toString();
            };
        }

        public String getFirstAndLastName(Map<String, Object> entity) {
            String preferred = entity.get("preferredName").toString();
            String[] lastAndFirst = preferred.split(", ");
            return lastAndFirst.length == 2 ? lastAndFirst[1] + " " + lastAndFirst[0] : preferred;
        }

        public boolean isLivingPerson(Map<String, Object> entity) {
            JsonNode json = new ObjectMapper().convertValue(entity, JsonNode.class);
            JsonNode dateOfBirth = json.get("dateOfBirth");
            JsonNode dateOfDeath = json.get("dateOfDeath");
            return json.get("type").toString().contains("DifferentiatedPerson")
                    && (dateOfBirth == null || getYear(dateOfBirth) > 1940)
                    && dateOfDeath == null;
        }

        private Integer getYear(JsonNode node) {
            String date = node.elements().next().textValue();
            String year = date.matches("\\d{4}-\\d{2}-\\d{2}") ? date.split("-")[0] : date;
            return asInt(year);
        }

        private Integer asInt(String year) {
            try {
                return Integer.parseInt(year);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private Function<Map<String, Object>, Mono<ServerResponse>> toResponse(
            String template, ServerRequest request) {
        return gndEntity -> {
            Map<String, Map<String, Object>> model =
                    Map.of("entity", sorted(gndEntity), "request", request.attributes());
            // Render Thymeleaf template (in src/main/resources/templates) with model:
            return ServerResponse.ok().render(template, model);
        };
    }

    private SortedMap<String, Object> sorted(Map<String, Object> gndEntity) {
        List<String> order = Arrays.asList(fieldOrder);
        SortedMap<String, Object> sortedMap =
                new TreeMap<>(
                        (field1, field2) -> {
                            int indexOf1 = order.indexOf(field1);
                            int indexOf2 = order.indexOf(field2);
                            // both unspecified, sort by field name:
                            if (indexOf1 == -1 && indexOf2 == -1) {
                                return field1.compareTo(field2);
                            }
                            // sort by order, unspecified after specified fields:
                            int end = Integer.MAX_VALUE;
                            return Integer.valueOf(indexOf1 == -1 ? end : indexOf1)
                                    .compareTo(Integer.valueOf(indexOf2 == -1 ? end : indexOf2));
                        });
        sortedMap.putAll(gndEntity);
        return sortedMap;
    }
}
