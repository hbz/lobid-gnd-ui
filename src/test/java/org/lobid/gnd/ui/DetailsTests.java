package org.lobid.gnd.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lobid.gnd.ui.config.HtmlUnitConfig;
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

    private static final String PRODUCTION = "https://lobid.org/gnd";
    private static final String DEVELOPMENT = "http://localhost";
    private static final String COLOGNE = "4031483-2";

    @Autowired private WebClient webClient;

    @LocalServerPort private int port;

    HtmlPage detailsPage;

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testDetailsRoute(String baseUrl) throws IOException {
        detailsPage = pageFor(baseUrl, COLOGNE);
        assertThat(detailsPage.getWebResponse().isSuccess());
        assertThat(detailsPage.getContentType()).isEqualTo(MediaType.TEXT_HTML_VALUE);
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testDetailsView(String baseUrl) throws IOException {
        detailsPage = pageFor(baseUrl, COLOGNE);
        assertThat(detailsPage.getTitleText()).isEqualTo("Köln");
        assertThat(detailsPage.asNormalizedText())
                .contains("https://d-nb.info/gnd/4031483-2")
                .contains("Köln")
                .contains("CCAA")
                .contains("Kolonie")
                .contains("Kūlūniyā")
                .contains("Nordrhein-Westfalen");
    }

    private HtmlPage pageFor(String baseUrl, String gndId) throws IOException {
        String baseUrlWithPort = baseUrl + (baseUrl.contains("localhost") ? ":" + port : "");
        return webClient.getPage(baseUrlWithPort + "/" + gndId);
    }
}
