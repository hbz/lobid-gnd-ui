package org.lobid.gnd_ui_lobid.util;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.util.Strings;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Provide attributes available in all templates.
 */
@ControllerAdvice
public class WebAdvice {

    @ModelAttribute("isDevserver")
    public Mono<Boolean> isDevserver(ServerWebExchange exchange) {
        return async(exchange, http -> optional(
                http.getRequest().getHeaders().get("X-Devserver")).equals("1"));
    }

    private Mono<Boolean> async(ServerWebExchange exchange,
            Function<ServerWebExchange, Boolean> isDevserver) {
        return Mono.just(exchange).map(isDevserver);
    }

    private String optional(List<String> values) {
        return Optional.ofNullable(values).map(List::getFirst).orElse(Strings.EMPTY);
    }
}
