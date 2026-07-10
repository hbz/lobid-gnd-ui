package org.lobid.gnd.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/* Tests for the `/gnd/reconcile` documentation page */
public class ReconcileDocTests extends HtmlPageTests {

    private static final String RECONCILE_DOC = "/reconcile";

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testReconcileDocPageTitle(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, RECONCILE_DOC).getTitleText()).isEqualTo("GND Reconciliation");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testReconcileDocPageHeaders(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, RECONCILE_DOC).asNormalizedText())
                .contains("GND Reconciliation")
                .contains("Daten und Werkzeuge")
                .contains("Tutorials")
                .contains("Community")
                .contains("API");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testGeneralApiDocumentation(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, RECONCILE_DOC).asNormalizedText())
                .contains("Service-URL:")
                .contains("curl")
                .contains("JSONP-callback")
                .contains("?callback=jsonp")
                .contains("CORS-header")
                .contains("grep");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testViewApiDocumentation(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, RECONCILE_DOC).asNormalizedText())
                .contains("View-API")
                .contains("Entities: view")
                .contains("/gnd/118624822")
                .contains("Entities: preview")
                .contains("/gnd/118624822.preview");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testQueryApiDocumentation(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, RECONCILE_DOC).asNormalizedText())
                .contains("Query-API")
                .contains("Query: GET")
                .contains("?queries=")
                .contains("Query: POST")
                .contains("--data 'queries=");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testSuggestApiDocumentation(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, RECONCILE_DOC).asNormalizedText())
                .contains("Suggest-API")
                .contains("Suggest: entity")
                .contains("Suggest: type")
                .contains("Suggest: property");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testSuggestEntityEndpoint(String baseUrl) throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl, "reconcile/suggest/entity?prefix=hbz"))
                .is(validJson())
                .contains("Hochschulbibliothekszentrum");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testSuggestTypeEndpoint(String baseUrl) throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl, "reconcile/suggest/type?prefix=werk"))
                .is(validJson())
                .contains("Werk der Musik");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testSuggestPropertyEndpoint(String baseUrl)
            throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl, "reconcile/suggest/property?prefix=beruf"))
                .is(validJson())
                .contains("Beruf oder Beschäftigung");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testFlyoutApiDocumentation(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, RECONCILE_DOC).asNormalizedText())
                .contains("Flyout")
                .contains("Flyout: entity")
                .contains("Flyout: type")
                .contains("Flyout: property");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testFlyoutEntityEndpoint(String baseUrl) throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl, "reconcile/flyout/entity?id=2047974-8"))
                .contains("Hochschulbibliothekszentrum");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testFlyoutTypeEndpoint(String baseUrl) throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl, "reconcile/flyout/type?id=Work")).contains("Werk");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testFlyoutPropertyEndpoint(String baseUrl)
            throws IOException, InterruptedException {
        String response =
                fetchHttpResponse(baseUrl, "reconcile/flyout/property?id=professionOrOccupation");
        assertThat(response).contains("Beruf oder Beschäftigung");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testDataExtensionApiDocumentation(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, RECONCILE_DOC).asNormalizedText())
                .contains("Data-extension-API")
                .contains("Property-proposals")
                .contains("Extend: GET")
                .contains("?extend=")
                .contains("Extend: POST")
                .contains("--data 'extend=");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testPropertyProposalsEndpoint(String baseUrl)
            throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl, "reconcile/properties?type=Work"))
                .is(validJson())
                .contains("Verfasser");
    }
}
