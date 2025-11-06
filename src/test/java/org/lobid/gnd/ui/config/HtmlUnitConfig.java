package org.lobid.gnd.ui.config;

import org.htmlunit.WebClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class HtmlUnitConfig {

    @Bean
    public WebClient webClient() {
        WebClient webClient = new WebClient();
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setPrintContentOnFailingStatusCode(true);
        return webClient;
    }
}
