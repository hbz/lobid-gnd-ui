package org.lobid.gnd.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.Condition;
import org.htmlunit.html.DomAttr;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlListItem;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/* Tests for the `/gnd/search` results page */
public class SearchTests extends HtmlPageTests {

    private static final String SEARCH = "/search";

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testSearchForm(String baseUrl) throws IOException {
        HtmlPage searchPage = pageFor(baseUrl, SEARCH);
        HtmlInput searchBox = searchPage.getFirstByXPath("//input[@id='gnd-query']");
        assertThat(searchBox).as("search box should exist").isNotNull();
        HtmlButton searchButton = searchPage.getFirstByXPath("//button[@title='Suchen']");
        assertThat(searchButton).as("search button should exist").isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testSearchFormClear(String baseUrl) throws IOException {
        HtmlPage searchPage = pageFor(baseUrl, SEARCH);
        HtmlInput searchBox = searchPage.getFirstByXPath("//input[@id='gnd-query']");
        searchBox.type("Test");
        assertThat(searchBox.getValue()).isEqualTo("Test");
        HtmlButton clearButton =
                searchPage.getFirstByXPath("//button[@class='ui-autocomplete-clear']");
        clearButton.click();
        assertThat(searchBox.getValue()).as("search box should be empty after clearing").isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testSearchResults(String baseUrl) throws IOException {
        HtmlPage searchPage = search("Make-Tuwen", baseUrl);
        DomAttr detailsLink = searchPage.getFirstByXPath("//a[text()='Twain, Mark']/@href");
        assertThat(detailsLink.getValue())
                .as("each result should link to its details page")
                .contains("gnd/118624822");
        String searchResults = searchPage.asNormalizedText();
        assertThat(searchResults)
                .as("the search has main navigation, results, and facets")
                .contains("Treffer pro Seite")
                .contains("1 Treffer, zeige 1 bis 1")
                .contains("Ergebnisse eingrenzen");
        assertThat(searchResults)
                .as("the results contains details for each entity")
                .contains("Twain, Mark")
                .contains("Individualisierte Person")
                .contains("Schriftsteller, Journalist, Drucker, Lotse, Soldat")
                .contains("1835–1910")
                .contains("118624822");
        assertThat(searchResults)
                .as("the facets contain values from the search results")
                .contains("Entitätstyp")
                .contains("Person")
                .contains("GND-Sachgruppe")
                .contains("Personen zu Literaturgeschichte (Schriftsteller)")
                .contains("Ländercode")
                .contains("USA")
                .contains("Beruf oder Beschäftigung");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testFacetLinks(String baseUrl) throws IOException {
        assertThat(search("Make-Tuwen", baseUrl))
                .has(linkFor("Person", "type", "Person"))
                .has(linkFor("Individualisierte Person", "type", "DifferentiatedPerson"))
                .has(
                        linkFor(
                                "Personen zu Literaturgeschichte",
                                "gndSubjectCategory.id",
                                "\"https://d-nb.info/standards/vocab/gnd/gnd-sc#12.2p\""))
                .has(
                        linkFor(
                                "USA",
                                "geographicAreaCode.id",
                                "\"https://d-nb.info/standards/vocab/gnd/geographic-area-code#XD-US\""))
                .has(
                        linkFor(
                                "Drucker",
                                "professionOrOccupation.id",
                                "\"https://d-nb.info/gnd/4013091-5\""))
                .has(
                        linkFor(
                                "Journalist",
                                "professionOrOccupation.id",
                                "\"https://d-nb.info/gnd/4028781-6\""))
                .has(
                        linkFor(
                                "Lotse",
                                "professionOrOccupation.id",
                                "\"https://d-nb.info/gnd/4036380-6\""))
                .has(
                        linkFor(
                                "Soldat",
                                "professionOrOccupation.id",
                                "\"https://d-nb.info/gnd/4055409-0\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testAutocomplete(String baseUrl) throws IOException {
        HtmlPage searchPage = pageFor(baseUrl, SEARCH);

        HtmlInput searchBox = searchPage.getFirstByXPath("//input[@id='gnd-query']");
        searchBox.type("Make-Tuwen");
        webClient.waitForBackgroundJavaScript(1000);
        HtmlListItem suggestion =
                searchPage.getFirstByXPath("//ul[contains(@class, 'ui-autocomplete')]/li");
        assertThat(suggestion.asNormalizedText())
                .as("suggestion should contain details")
                .contains("Twain, Mark | 1835–1910")
                .contains("Schriftsteller; Journalist; Drucker; Lotse; Soldat");

        HtmlPage detailsPage = suggestion.click();
        webClient.waitForBackgroundJavaScript(10);
        assertThat(detailsPage.asNormalizedText())
                .as("details page for selected suggestion should be open")
                .contains("https://d-nb.info/gnd/118624822")
                .contains("Snodgrass, Quintus Curtius");
    }

    private HtmlPage search(String searchQuery, String baseUrl) throws IOException {
        HtmlPage searchPage = pageFor(baseUrl, SEARCH);
        HtmlInput searchBox = searchPage.getFirstByXPath("//input[@id='gnd-query']");
        HtmlButton searchButton = searchPage.getFirstByXPath("//button[@title='Suchen']");
        searchBox.type(searchQuery);
        return searchButton.click();
    }

    private Condition<HtmlPage> linkFor(String text, String field, String value) {
        String filterParam = String.format("%s:%s", field, value);
        return new Condition<>(
                page -> hasLink(text, filterParam, page), "link for '%s' with: %s", text, field);
    }

    private boolean hasLink(String text, String url, HtmlPage page) {
        DomAttr link = page.getFirstByXPath("//a[contains(text(), '" + text + "')]/@href");
        String filter = URLEncoder.encode(String.format("+(%s)", url), StandardCharsets.UTF_8);
        assertThat(link.getValue()).contains(String.format("filter=%s", filter));
        return true;
    }
}
