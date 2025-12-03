package org.lobid.gnd.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import org.htmlunit.ElementNotFoundException;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlDivision;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/* Tests for the `details.html` template */
public class DetailsTests extends HtmlPageTests {

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testDetailsViewFields(String baseUrl) throws IOException {
        HtmlPage detailsPage = pageFor(baseUrl, COLOGNE);
        assertThat(detailsPage.getTitleText()).isEqualTo("Köln");
        assertThat(detailsPage.asNormalizedText())
                .contains("https://d-nb.info/gnd/4031483-2")
                .contains("Köln")
                .contains("CCAA")
                .contains("Kolonie")
                .contains("Kūlūniyā")
                .contains("Nordrhein-Westfalen");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testDetailsViewHeader(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, COLOGNE).getElementsByTagName("h1").getFirst().getTextContent())
                .contains("Köln")
                .contains("Gebietskörperschaft oder Verwaltungseinheit")
                .contains("Geografikum")
                .contains("Hauptstadt des Regierungsbezirks Köln");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testDetailsViewLinks(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, COLOGNE).getElementsByTagName("a").toString())
                .contains("TerritorialCorporateBodyOrAdministrativeUnit")
                .contains("PlaceOrGeographicName")
                .contains("4031483-2.json")
                .contains("https://www.stadt-koeln.de/")
                .contains("https://d-nb.info/standards/vocab/gnd/geographic-area-code#XA-DE-NW")
                .contains("https://www.herder-institut.de/bildkatalog/gnd/4031483-2")
                .contains("https://www.dnb.de/lds")
                .contains("https://d-nb.info/gnd/4031483-2/about/lds.rdf")
                .contains("https://d-nb.info/gnd/4031483-2/about/lds.ttl")
                .contains("https://www.dnb.de/entityfacts")
                .contains("http://hub.culturegraph.org/entityfacts/4031483-2")
                .contains("https://creativecommons.org/publicdomain/zero/1.0/");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testDetailsViewMap(String baseUrl) throws IOException {
        List<DomElement> mapElements = pageFor(baseUrl, COLOGNE).getElementsById("authority-map");
        assertThat(mapElements).isNotEmpty();
        assertThat(mapElements.getFirst().getElementsByTagName("a").toString())
                .contains("http://leafletjs.com")
                .contains("http://osm.org/copyright");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testDetailsViewImage(String baseUrl) throws IOException {
        HtmlPage detailsPage = pageFor(baseUrl, COLOGNE);
        assertThat(detailsPage.getByXPath("//img[@alt='Köln']")).isNotEmpty();
        assertThat(detailsPage.getBody().asNormalizedText())
                .contains("Bildquelle")
                .contains("Wikimedia Commons")
                .contains("CC");
        assertThat(detailsPage.getElementsByTagName("a").toString())
                .contains("https://commons.wikimedia.org")
                .contains("https://creativecommons.org");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testDetailsViewPersonHistorical(String baseUrl) throws IOException {
        HtmlPage detailsPage = pageFor(baseUrl, PERSON_HISTORICAL);
        assertThat(detailsPage.asNormalizedText())
                .contains("Beruf oder Beschäftigung")
                .contains("Adelstitel")
                .contains("Geburtsdatum")
                .contains("Sterbedatum")
                .contains("Beziehung, Bekanntschaft, Freundschaft")
                .contains("Titelangabe");
        assertThrows(
                ElementNotFoundException.class,
                () -> detailsPage.getHtmlElementById("meta-person"));
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testDetailsViewPersonAlive(String baseUrl) throws IOException {
        HtmlPage detailsPage = pageFor(baseUrl, PERSON_ALIVE);
        HtmlDivision personBox = detailsPage.getHtmlElementById("meta-person");
        assertThat(personBox).isNotNull();
        HtmlAnchor link = (HtmlAnchor) personBox.getByXPath("//a[@data-toggle='collapse']").get(0);
        assertThat(link).isNotNull();
        assertThat(link.getTextContent()).contains("Sind Sie").contains("Klicken Sie hier");
        String text = "Diese Seite zeigt einen Datensatz aus der Gemeinsamen Normdatei";
        assertThat(detailsPage.asNormalizedText()).doesNotContain(text);
        assertThat(((HtmlPage) link.click()).asNormalizedText()).contains(text);
    }
}
