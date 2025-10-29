package org.lobid.gnd_ui_lobid;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lobid.gnd_ui_lobid.config.HtmlUnitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(HtmlUnitConfig.class)
public class DetailsTests {

    // Test rendered templates, using HtmlUnit:
    // https://www.htmlunit.org/gettingStarted.html

    @Autowired
    private WebClient webClient;

    @LocalServerPort
    private int port;

    HtmlPage detailsPage;

    @BeforeEach
    public void setUp() throws IOException {
        detailsPage = webClient.getPage("http://localhost:" + port + "/4031483-2");
    }

    @Test
    public void testDetailsRoute() throws IOException {
        assertThat(detailsPage.getWebResponse().isSuccess());
        assertThat(detailsPage.getContentType())
                .isEqualTo(MediaType.TEXT_HTML_VALUE);
    }

    @Test
    public void testDetailsView() throws Exception {
        assertThat(detailsPage.getTitleText())
                .isEqualTo("4031483-2 - gnd-ui-lobid");
        assertThat(detailsPage.asNormalizedText())
                .contains("https://d-nb.info/gnd/4031483-2")
                .contains("Köln");
    }
}
