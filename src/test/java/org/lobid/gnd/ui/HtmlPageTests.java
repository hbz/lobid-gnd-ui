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
/* Superclass for template tests */
public abstract class HtmlPageTests {

    // Test rendered templates, using HtmlUnit:
    // https://www.htmlunit.org/gettingStarted.html

    protected static final String PRODUCTION = "https://lobid.org";
    protected static final String DEVELOPMENT = "http://localhost";
    protected static final String COLOGNE = "4031483-2";
    protected static final String PERSON_HISTORICAL = "118637649";
    protected static final String PERSON_ALIVE = "122729501";
    protected static final String PERSON_WITH_RELATIONSHIPS = "118548018";

    @Autowired private WebClient webClient;

    @LocalServerPort private int port;

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testRoute(String baseUrl) throws IOException {
        HtmlPage testPage = pageFor(baseUrl, COLOGNE);
        assertThat(testPage.getWebResponse().isSuccess());
        assertThat(testPage.getContentType()).isEqualTo(MediaType.TEXT_HTML_VALUE);
    }

    protected HtmlPage pageFor(String baseUrl, String gndId) throws IOException {
        String baseUrlWithPort = baseUrl + (baseUrl.contains("localhost") ? ":" + port : "");
        return webClient.getPage(baseUrlWithPort + "/gnd/" + gndId);
    }
}
