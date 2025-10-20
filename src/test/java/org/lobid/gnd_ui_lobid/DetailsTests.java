package org.lobid.gnd_ui_lobid;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class DetailsTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebClient webClient;

    @Test
    public void testDetailsRoute() throws Exception {
        // Test routing in controller:
        mockMvc.perform(get("/4031483-2"))
                .andExpect(status().isOk())
                .andExpect(view().name("details"));
    }

    @Test
    public void testDetailsModel() throws Exception {
        // Test UI model passed from controller to templates:
        mockMvc.perform(get("/4031483-2"))
                .andExpect(model().attribute("id", "https://d-nb.info/gnd/4031483-2"))
                .andExpect(model().attribute("gndIdentifier", "4031483-2"))
                .andExpect(model().attribute("preferredName", "Köln"));
    }

    @Test
    public void testDetailsView() throws Exception {
        // Test rendered templates, using HtmlUnit:
        // https://www.htmlunit.org/gettingStarted.html
        HtmlPage detailsPage = webClient.getPage("/4031483-2");
        assertThat(detailsPage.getTitleText(), is("4031483-2 - gnd-ui-lobid"));
        final String pageText = detailsPage.asNormalizedText();
        assertThat(pageText, containsString("https://d-nb.info/gnd/4031483-2"));
        assertThat(pageText, containsString("Köln"));
    }

}
