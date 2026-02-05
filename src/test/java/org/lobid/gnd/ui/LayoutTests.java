package org.lobid.gnd.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/* Tests for the `layout.html` template */
public class LayoutTests extends HtmlPageTests {

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testLayoutContent(String baseUrl) throws IOException {
        HtmlPage testPage = pageFor(baseUrl, COLOGNE);
        assertThat(testPage.asNormalizedText())
                .contains("gnd")
                .contains("Erkunden")
                .contains("API")
                .contains("hbz")
                .contains("Gewährleistung")
                .contains("Impressum")
                .contains("Datenschutz")
                .contains("GitHub")
                .contains("Blog");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testLayoutForm(String baseUrl) throws IOException {
        List<HtmlForm> forms = pageFor(baseUrl, COLOGNE).getForms();
        assertThat(forms).isNotEmpty();
        assertThat(forms.getFirst().getInputsByName("q")).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testLayoutLinks(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, COLOGNE).getElementsByTagName("a").toString())
                .contains("/gnd")
                .contains("/gnd/search")
                .contains("/gnd/api")
                .contains("/gnd/dataset")
                .contains("/gnd/reconcile")
                .contains("https://www.hbz-nrw.de/produkte/linked-open-data")
                .contains("http://lobid.org/warranty")
                .contains("http://www.hbz-nrw.de/impressum")
                .contains("https://github.com/hbz/lobid/blob/master/conf/Datenschutzerklaerung")
                .contains("https://openbiblio.social/@lobid")
                .contains("http://github.com/hbz/lobid-gnd")
                .contains("http://blog.lobid.org/");
    }
}
