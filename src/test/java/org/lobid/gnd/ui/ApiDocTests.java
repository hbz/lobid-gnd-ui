package org.lobid.gnd.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlListItem;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/* Tests for the `/gnd/api` documentation page */
public class ApiDocTests extends HtmlPageTests {

    private static final String API_DOC = "/api";

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testApiDocPageTitle(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).getTitleText()).isEqualTo("lobid-gnd - API");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testApiDocPageHeaders(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("lobid-gnd API")
                .contains("Richtlinien zur API-Nutzung")
                .contains("Suche: /gnd/search?q=text")
                .contains("Direktzugriff: /gnd/<id>.json")
                .contains("Inhaltstypen")
                .contains("Bulk-Downloads")
                .contains("Autovervollständigung")
                .contains("JSON-LD")
                .contains("OpenRefine");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testSearchAllExample(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("Alles")
                .contains("/gnd/search?q=*&format=json");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testSearchAllJsonEndpoint(String baseUrl) throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl + "/gnd/search?q=*&format=json"))
                .is(validJson())
                .contains("\"totalItems\"")
                .contains("\"member\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testSearchAllFieldsExample(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("Alle Felder")
                .contains("/gnd/search?q=london&format=json");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testSearchAllFieldsJsonEndpoint(String baseUrl)
            throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl + "/gnd/search?q=london&format=json"))
                .is(validJson())
                .contains("\"totalItems\"")
                .contains("London");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testFieldSearchExample(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("Feldsuche")
                .contains("/gnd/search?q=preferredName:Twain&format=json");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testFilterSearchExample(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("Filter")
                .contains("filter=type:Person");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testPaginationExample(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("Paginierung")
                .contains("from=50")
                .contains("size=100");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testSortingExample(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("Sortierung")
                .contains("sort=preferredName.keyword:asc");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testAsciiSearchExample(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("ASCII")
                .contains("preferredName.ascii:Chor")
                .contains("variantName.ascii:Chor");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testDirectAccessExamples(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("Direktzugriff: /gnd/<id>.json")
                .contains("London")
                .contains("/gnd/4074335-4.json")
                .contains("hbz")
                .contains("/gnd/2047974-8.json")
                .contains("Goethe")
                .contains("/gnd/118540238.json");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testDirectAccessLondonJsonEndpoint(String baseUrl)
            throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl + "/gnd/4074335-4.json"))
                .is(validJson())
                .contains("\"id\"")
                .contains("4074335-4");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testContentTypeDocumentation(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("Content-Negotiation")
                .contains("Accept-Header")
                .contains("application/json")
                .contains("text/html")
                .contains("format=json");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testRdfSerializationDocumentation(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("RDF")
                .contains("application/rdf+xml")
                .contains("text/turtle")
                .contains("application/n-triples")
                .contains(".rdf")
                .contains(".ttl")
                .contains(".nt");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testRdfXmlFormatEndpoint(String baseUrl) throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl + "/gnd/4074335-4.rdf"))
                .contains("rdf:RDF")
                .contains("4074335-4");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testTurtleFormatEndpoint(String baseUrl) throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl + "/gnd/4074335-4.ttl"))
                .contains("@prefix")
                .contains("4074335-4");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testNTriplesFormatEndpoint(String baseUrl)
            throws IOException, InterruptedException {
        String response = fetchHttpResponse(baseUrl + "/gnd/4074335-4.nt");
        assertThat(response).contains("4074335-4");
        assertThat(response.split("\n").length).isGreaterThan(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testBulkDownloadsDocumentation(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("Bulk-Downloads")
                .contains("JSON lines")
                .contains("application/x-jsonlines")
                .contains("format=jsonl")
                .contains("gzip")
                .contains("Accept-Encoding");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testBulkDownloadJsonLinesEndpoint(String baseUrl)
            throws IOException, InterruptedException {
        String[] lines =
                fetchHttpResponse(baseUrl + "/gnd/search?q=type:Country&format=jsonl").split("\n");
        assertThat(lines.length).isGreaterThan(0);
        assertThat(lines[0]).is(validJson());
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testAutocompleteDocumentation(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("Autovervollständigung")
                .contains("json:suggest")
                .contains("json:preferredName")
                .contains("professionOrOccupation")
                .contains("Feld-Templates");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testAutocompleteCodeExample(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("$('input.search-gnd').autocomplete")
                .contains("url : \"/gnd/search\"")
                .contains("dataType : \"jsonp\"")
                .contains("q : request.term")
                .contains("format : \"json:preferredName,professionOrOccupation\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testAutocompleteSuggestEndpoint(String baseUrl)
            throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl + "/gnd/search?q=Twain&format=json:suggest"))
                .is(validJson())
                .contains("Twain, Mark");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testAutocompleteSuggestExample(String baseUrl) throws IOException {
        HtmlPage apiPage = pageFor(baseUrl, API_DOC);

        HtmlInput labelInput = apiPage.getFirstByXPath("//input[@id='label']");
        assertThat(labelInput).as("form input for label should exist").isNotNull();
        HtmlInput idInput = apiPage.getFirstByXPath("//input[@id='id']");
        assertThat(idInput).as("form input for ID should exist").isNotNull();
        HtmlButton searchButton = apiPage.getFirstByXPath("//button[contains(text(), 'Suchen')]");
        assertThat(searchButton).as("search button should exist").isNotNull();

        labelInput.type("Make-Tuwen");
        webClient.waitForBackgroundJavaScript(1000);
        HtmlListItem suggestion =
                apiPage.getFirstByXPath("//ul[contains(@class, 'ui-autocomplete')]/li");
        suggestion.click();
        webClient.waitForBackgroundJavaScript(10);

        assertThat(labelInput.getValue())
                .as("form should be filled with details for selected suggestion")
                .contains("Twain, Mark | Schriftsteller; Journalist; Drucker; Lotse; Soldat");
        assertThat(idInput.getValue())
                .as("form should be filled with ID search for selected suggestion")
                .contains("id:\"https://d-nb.info/gnd/118624822\"");
        HtmlPage searchResults = searchButton.click();
        assertThat(searchResults.asNormalizedText())
                .as("search results should contain the label for the searched ID")
                .contains("Twain, Mark");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testJsonLdDocumentation(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("JSON-LD")
                .contains("JSON-LD Playground")
                .contains("JSON-LD Context")
                .contains("/gnd/context.jsonld")
                .contains("RDF-Konvertierung")
                .contains("jsonld-cli")
                .contains("N-Quads");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testJsonLdContextEndpoint(String baseUrl) throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl + "/gnd/context.jsonld"))
                .is(validJson())
                .contains("\"@context\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testJsonLdCodeExamples(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("jsonld format --nquads https://lobid.org/gnd/4074335-4")
                .contains("curl --header \"Accept: application/rdf+xml\"")
                .contains("curl --header \"Accept: text/turtle\"")
                .contains("curl --header \"Accept: application/n-triples\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testOpenRefineDocumentation(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).asNormalizedText())
                .contains("OpenRefine")
                .contains("GND reconciliation");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION /*, DEVELOPMENT*/})
    public void testApiDocLinksExist(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, API_DOC).getElementsByTagName("a").toString())
                .contains("/gnd/search?q=")
                .contains("/gnd/4074335-4")
                .contains("/gnd/context.jsonld")
                .contains("http://json-ld.org/playground/")
                .contains("https://github.com/digitalbazaar/jsonld-cli")
                .contains("/gnd/reconcile")
                .contains("http://lobid.org/usage-policy")
                .contains("http://blog.lobid.org");
    }
}
