package org.lobid.gnd_ui_lobid.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "home/index";
    }

    // Define URL route for GND entry with ID, e.g. `/4031483-2`:
    @GetMapping(value = "/{id}", produces = "text/html")
    public String details(Model model, @PathVariable String id) {

        // Get JSON data from lobid-gnd:
        JsonNode json = RestClient.create().get()
                .uri("https://lobid.org/gnd/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().body(JsonNode.class);

        // Pass JSON data to UI model:
        @SuppressWarnings("unchecked")
        Map<String, Object> map = new ObjectMapper().convertValue(json, Map.class);
        model.addAllAttributes(map);

        // Render Thymeleaf template details.html (in src/main/resources/templates):
        return "details";
    }

}
