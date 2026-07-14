package org.lobid.gnd.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/* Tests for the `/gnd/dataset` documentation page */
public class DatasetDocTests extends HtmlPageTests {

    private static final String DATASET_DOC = "/dataset";

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testDatasetDocPageTitle(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, DATASET_DOC).getTitleText()).isEqualTo("Dataset: lobid-gnd");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testDatasetDocPageHeaders(String baseUrl) throws IOException {
        String text = pageFor(baseUrl, DATASET_DOC).asNormalizedText();
        assertThat(text).contains("Daten").contains("API");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testDatasetDescription(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, DATASET_DOC).asNormalizedText())
                .contains("Gemeinsame Normdatei (GND)")
                .contains("8 Millionen")
                .contains("Katalogisierung")
                .contains("in verschiedenen Kontexten");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testDatasetMetadata(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, DATASET_DOC).asNormalizedText())
                .contains("lobid-gnd")
                .contains("LOD-API für die Gemeinsame Normdatei (GND)")
                .contains("Schlagwörter")
                .contains("authority data")
                .contains("Germany")
                .contains("Austria")
                .contains("Switzerland")
                .contains("Veröffentlicht von")
                .contains("Hochschulbibliothekszentrum")
                .contains("hbz")
                .contains("Basiert auf")
                .contains("Gemeinsame Normdatei (GND)")
                .contains("Veröffentlicht am")
                .contains("2018-07-11")
                .contains("Sprache")
                .contains("de")
                .contains("Ergänzungsfrequenz")
                .contains("Stündlich")
                .contains("Kontakt");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testDatasetApiSection(String baseUrl) throws IOException {
        assertThat(pageFor(baseUrl, DATASET_DOC).asNormalizedText())
                .contains("lobid-gnd-API")
                .contains("Zugriff auf strukturierte Daten")
                .contains("JSON-LD")
                .contains("https://lobid.org/gnd/api")
                .contains("https://lobid.org/gnd/search")
                .contains("application/json, application/ld+json");
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTION, DEVELOPMENT})
    public void testDatasetJsonLdEndpoint(String baseUrl) throws IOException, InterruptedException {
        assertThat(fetchHttpResponse(baseUrl, "dataset.jsonld"))
                .is(validJson())
                .contains("\"type\":\"Dataset\"");
    }
}
