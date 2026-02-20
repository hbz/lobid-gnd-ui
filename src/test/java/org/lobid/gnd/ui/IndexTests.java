package org.lobid.gnd.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.htmlunit.html.HtmlFigureCaption;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlScript;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/* Tests for the `index.html` template */
public class IndexTests extends HtmlPageTests {

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testIndexContent(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, "").asNormalizedText())
                .contains("Rechercheoberfläche & LOD-API für die GND")
                .contains("Die Gemeinsame Normdatei (GND) enthält über 8 Millionen Normdatensätze")
                .contains("Die GND enthält normierte Einträge für Personen, Körperschaften")
                .contains("lobid-gnd bietet eine Rechercheoberfläche zum Durchsuchen der GND");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testIndexLinks(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, "").getElementsByTagName("a").toString())
                .contains("https://www.dnb.de/lds#doc58246bodyText1")
                .contains("https://www.dnb.de/entityfacts");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testIndexImages(String baseUrl) throws IOException {
        HtmlPage indexPage = pageFor(baseUrl, "");
        assertThat(indexPage.getElementsByTagName("img").toString())
                .contains("hbz.png")
                .contains("https://commons.wikimedia.org/wiki/Special:FilePath");
        HtmlFigureCaption figcaption = indexPage.getFirstByXPath("//figcaption");
        assertThat(figcaption.getTextContent())
                .contains("Bildquelle")
                .contains("Wikimedia Commons");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testJsonLdScript(String baseUrl) throws IOException {
        HtmlScript jsonLdScript =
                pageFor(baseUrl, "").getFirstByXPath("//script[@type='application/ld+json']");
        assertThat(jsonLdScript.getTextContent())
                .isEqualToIgnoringWhitespace(
"""
{
    "@context": "http://schema.org",
    "@type": "WebSite",
    "url": "https://lobid.org/gnd",
    "potentialAction": {
        "@type": "SearchAction",
        "target": "https://lobid.org/gnd/search?q={search_term_string}",
        "query-input": "required name=search_term_string"
    }
}
""");
    }
}
