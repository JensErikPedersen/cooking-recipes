package dk.serik.recipes.exceptions;

import dk.serik.recipes.controllers.CategoryController;
import dk.serik.recipes.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Requests that Spring MVC itself rejects, before any controller runs. Each exception carries its
 * own HTTP status; the catch-all in ApplicationExceptionHandler must not turn it into a 500.
 */
@WebMvcTest(CategoryController.class)
public class FrameworkErrorStatusTest {

    private static final String BASE = "/api/v1/categories";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("Given a path nothing handles, When requested, Then 404 in the error envelope")
    public void shouldReturn404ForUnknownPath() throws Exception {
        mockMvc.perform(get("/api/v1/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.REQUEST_REJECTED.getCode()));
    }

    @Test
    @DisplayName("Given a method the path does not support, When requested, Then 405")
    public void shouldReturn405ForUnsupportedMethod() throws Exception {
        mockMvc.perform(delete(BASE))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Given a body that is not JSON, When POSTed, Then 415")
    public void shouldReturn415ForUnsupportedContentType() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.TEXT_PLAIN).content("Dessert"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Given malformed JSON, When POSTed, Then 400")
    public void shouldReturn400ForMalformedJson() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
                .andExpect(status().isBadRequest());
    }
}
