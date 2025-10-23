package org.lobid.gnd_ui_lobid;

import static org.assertj.core.api.Assertions.assertThat;

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lobid.gnd_ui_lobid.config.HtmlUnitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(HtmlUnitConfig.class)
public class DetailsTests {

    // Test via WebFlux WebTestClient:

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    public void testDetailsRoute() {
        webTestClient.get().uri("/4031483-2")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus()
                .is2xxSuccessful();
    }

    // Test via HtmlUnit WebClient:

    @Autowired
    private WebClient webClient;

    @LocalServerPort
    private int port;

    @Test
    public void testDetailsView() throws Exception {
        // Test rendered templates, using HtmlUnit:
        // https://www.htmlunit.org/gettingStarted.html
        HtmlPage detailsPage = webClient.getPage("http://localhost:" + port + "/4031483-2");
        assertThat(detailsPage.getTitleText())
                .isEqualTo("4031483-2 - gnd-ui-lobid");
        assertThat(detailsPage.asNormalizedText())
                .contains("https://d-nb.info/gnd/4031483-2")
                .contains("Köln");
    }
}
