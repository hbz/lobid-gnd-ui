package org.lobid.gnd_ui_lobid.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.Rendering;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "home/index";
    }

    // Define URL route for GND entry with ID, e.g. `/4031483-2`:
    @GetMapping(value = "/{id}")
    public Mono<Rendering> details(Model model, @PathVariable String id) {

        // Get JSON data from lobid-gnd:
        Mono<JsonNode> jsonMono = WebClient.create().get()
                .uri("https://lobid.org/gnd/{id}", id)
                .accept(MediaType.APPLICATION_JSON).retrieve()
                .bodyToMono(JsonNode.class);

        // Convert JSON data to Java Map (to be passed to template):
        Mono<Map<String, Object>> mapMono = jsonMono
                .map(json -> new ObjectMapper().convertValue(json, new TypeReference<>() {}));

        // Render Thymeleaf template details.html (in src/main/resources/templates):
        return mapMono.map(javaMap -> Rendering.view("details").model(javaMap).build());
    }

}
