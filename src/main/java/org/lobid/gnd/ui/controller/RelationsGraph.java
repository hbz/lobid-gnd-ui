package org.lobid.gnd.ui.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Component("relations")
/* Create vis-network JSON data for graph visualization of entity relations. */
public class RelationsGraph {

    private static final String GND_PREFIX = "https://d-nb.info/gnd/";

    private static final Set<String> SKIP_FIELDS =
            Set.of(
                    "id", "type",
                    "sameAs", "isPartOf",
                    "label", "preferredName",
                    "gndIdentifier", "labels");

    public static boolean hasGndRelations(Map<String, Object> entity) {
        return entity.entrySet().stream()
                .filter(entry -> !SKIP_FIELDS.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(map -> map.containsKey("id"))
                .map(map -> String.valueOf(map.get("id")))
                .anyMatch(uri -> uri.contains(GND_PREFIX));
    }

    public String gndRelationNodes(Map<String, Object> entity) {
        JsonNode json = new ObjectMapper().convertValue(entity, JsonNode.class);
        List<Map<String, Object>> result = new ArrayList<>();
        addGndEntityNodes(json, result);
        addGroupingNodes(json, result);
        return new ObjectMapper().valueToTree(result).toString();
    }

    public String gndRelationEdges(Map<String, Object> entity) {
        JsonNode json = new ObjectMapper().convertValue(entity, JsonNode.class);
        List<Map<String, Object>> result = new ArrayList<>();
        addDirectConnections(json, result);
        addGroupedConnections(json, result);
        return new ObjectMapper().valueToTree(result).toString();
    }

    private void addGndEntityNodes(JsonNode json, List<Map<String, Object>> result) {
        Map<String, Object> mainEntityNode =
                Map.of(
                        "id", text(json, "gndIdentifier"),
                        "label", wrap(text(json, "preferredName")),
                        "shape", "box");
        result.add(mainEntityNode);
        gndNodes(json)
                .flatMap(pair -> pair.getT2().stream())
                .distinct()
                .filter(node -> node.get("id") != null && node.get("label") != null)
                .forEach(addAsEntityNode(result));
    }

    private Consumer<JsonNode> addAsEntityNode(List<Map<String, Object>> result) {
        return node -> {
            String id = text(node, "id").substring(GND_PREFIX.length());
            String label = wrap(text(node, "label"));
            String title = "Details zu " + text(node, "label") + " öffnen";
            result.add(Map.of("id", id, "label", label, "shape", "box", "title", title));
        };
    }

    private void addGroupingNodes(JsonNode json, List<Map<String, Object>> result) {
        gndNodes(json)
                .filter(pair -> pair.getT2().size() > 1)
                .map(Tuple2::getT1)
                .distinct()
                .forEach(addAsGroupingNode(json, result));
    }

    private Consumer<String> addAsGroupingNode(JsonNode json, List<Map<String, Object>> result) {
        return rel -> {
            String label = wrap(text(json.get("labels"), rel));
            result.add(Map.of("id", rel, "shape", "dot", "size", "5", "label", label));
        };
    }

    private Stream<Tuple2<String, List<JsonNode>>> gndNodes(JsonNode json) {
        return asStream(json.fieldNames()).filter(isGndRelation(json)).map(toTuple(json));
    }

    private Predicate<? super String> isGndRelation(JsonNode json) {
        return key -> {
            JsonNode node = json.get(key);
            return !SKIP_FIELDS.contains(key)
                    && node.isArray()
                    && node.size() > 0
                    && node.elements().next().isObject()
                    && node.toString().contains(GND_PREFIX);
        };
    }

    private Function<String, Tuple2<String, List<JsonNode>>> toTuple(JsonNode json) {
        return key ->
                Tuples.of(key, asStream(json.get(key).elements()).collect(Collectors.toList()));
    }

    private void addGroupedConnections(JsonNode json, List<Map<String, Object>> result) {
        gndNodes(json)
                .filter(pair -> pair.getT2().size() > 1)
                .forEach(addAsConnectionToGroupingNode(json, result));
    }

    private void addDirectConnections(JsonNode json, List<Map<String, Object>> result) {
        gndNodes(json)
                .filter(pair -> pair.getT2().size() == 1)
                .forEach(addAsConnectionToEntityNode(json, result));
    }

    private Consumer<Tuple2<String, List<JsonNode>>> addAsConnectionToGroupingNode(
            JsonNode json, List<Map<String, Object>> result) {
        return pair -> {
            String to = pair.getT1();
            String from = text(json, "gndIdentifier");
            result.add(Map.of("from", from, "to", to));
            pair.getT2().stream()
                    .filter(node -> node.get("id") != null)
                    .forEach(addAsConnectionFromGroupingToEntityNode(json, result, to));
        };
    }

    private Consumer<JsonNode> addAsConnectionFromGroupingToEntityNode(
            JsonNode json, List<Map<String, Object>> result, String from) {
        return node -> {
            String to = text(node, "id").substring(GND_PREFIX.length());
            String title =
                    String.format(
                            "Einträge mit %s '%s' suchen",
                            text(json.get("labels"), from), text(node, "label"));
            String id = from + "_" + to;
            Map<String, Object> map =
                    Map.of(
                            "from", from,
                            "to", to,
                            "arrows", "to",
                            "id", id,
                            "title", title);
            result.add(map);
        };
    }

    private Consumer<Tuple2<String, List<JsonNode>>> addAsConnectionToEntityNode(
            JsonNode json, List<Map<String, Object>> result) {
        return pair -> {
            JsonNode target = pair.getT2().get(0);
            String to = text(target, "id").substring(GND_PREFIX.length());
            String rel = pair.getT1();
            String label = wrap(text(json.get("labels"), rel));
            String title =
                    String.format(
                            "Einträge mit %s '%s' suchen",
                            text(json.get("labels"), rel), text(target, "label"));
            String id = rel + "_" + to;
            String from = text(json, "gndIdentifier");
            Map<String, Object> map =
                    Map.of(
                            "from", from,
                            "to", to,
                            "arrows", "to",
                            "label", label,
                            "id", id,
                            "title", title);
            result.add(map);
        };
    }

    private String text(JsonNode json, String key) {
        return json.get(key).textValue();
    }

    private String wrap(String s) {
        return s.replaceAll("\\([^)]+\\)", "").replace(" ", "\n");
    }

    private <T> Stream<T> asStream(Iterator<T> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
}
