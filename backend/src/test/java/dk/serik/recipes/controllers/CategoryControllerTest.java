package dk.serik.recipes.controllers;

import tools.jackson.databind.json.JsonMapper;
import dk.serik.recipes.dto.CategoryDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mockutil.MockCategoryUtil;
import dk.serik.recipes.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
// The security chain is AuthenticationIT's business; these tests are about the controller.
@AutoConfigureMockMvc(addFilters = false)
public class CategoryControllerTest {

    private static final String BASE = "/api/v1/categories";
    private static final String ID = "913a5159-3717-4b9d-a290-0158d31ea8aa";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("Given categories exist, When GET the collection, Then 200 and the list is returned")
    public void shouldReturnAllCategories() throws Exception {
        given(categoryService.findAll()).willReturn(List.of(MockCategoryUtil.mockToBeSavedDessertDTO()));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Dessert"));
    }

    @Test
    @DisplayName("Given no categories, When GET the collection, Then 200 and an empty list")
    public void shouldReturnEmptyListWhenNoCategories() throws Exception {
        given(categoryService.findAll()).willReturn(List.of());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Given an existing category, When GET by id, Then 200 and the category is returned")
    public void shouldReturnCategoryById() throws Exception {
        given(categoryService.findById(ID)).willReturn(Optional.of(MockCategoryUtil.mockToBeSavedDessertDTO()));

        mockMvc.perform(get(BASE + "/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dessert"));
    }

    @Test
    @DisplayName("Given an unknown category, When GET by id, Then 404 and the error envelope")
    public void shouldReturn404WhenCategoryUnknown() throws Exception {
        given(categoryService.findById(ID)).willReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.CATEGORY_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(containsString("could not be found")));
    }

    @Test
    @DisplayName("Given a malformed id, When GET by id, Then 400 from the service-layer id check")
    public void shouldReturn400ForMalformedId() throws Exception {
        given(categoryService.findById("not-a-uuid"))
                .willThrow(ServiceException.badRequest(ApplicationErrorCodes.CATEGORY_ID_IS_NULL,
                        "Category Id 'not-a-uuid' is not a valid UUID"));

        mockMvc.perform(get(BASE + "/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("not a valid UUID")));
    }

    @Test
    @DisplayName("Given a valid payload, When POST, Then 201 with a Location header")
    public void shouldCreateCategory() throws Exception {
        given(categoryService.save(any())).willReturn(MockCategoryUtil.mockToBeSavedDessertDTO());

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                CategoryDTO.builder().name("Dessert").description("Den søde afrundning").build())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/" + ID)))
                .andExpect(jsonPath("$.name").value("Dessert"));
    }

    @Test
    @DisplayName("Given a payload with no name, When POST, Then 400 and the service is never called")
    public void shouldRejectCreateWithoutName() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                CategoryDTO.builder().description("no name supplied").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()))
                .andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"));

        verify(categoryService, never()).save(any());
    }

    @Test
    @DisplayName("Given a valid payload, When PUT, Then 200 and the path id wins over the body id")
    public void shouldUpdateCategoryUsingPathId() throws Exception {
        given(categoryService.update(any())).willReturn(MockCategoryUtil.mockToBeSavedDessertDTO());

        mockMvc.perform(put(BASE + "/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CategoryDTO.builder()
                                .id("00000000-0000-0000-0000-000000000000")   // deliberately different
                                .name("Dessert")
                                .build())))
                .andExpect(status().isOk());

        ArgumentCaptor<CategoryDTO> captor = ArgumentCaptor.forClass(CategoryDTO.class);
        verify(categoryService).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(ID);
    }

    @Test
    @DisplayName("Given an unknown category, When PUT, Then the service's 404 is surfaced")
    public void shouldReturn404WhenUpdatingUnknownCategory() throws Exception {
        given(categoryService.update(any())).willThrow(ServiceException.builder()
                .message("Category with id " + ID + " could not be found")
                .code(ApplicationErrorCodes.CATEGORY_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build());

        mockMvc.perform(put(BASE + "/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CategoryDTO.builder().name("Dessert").build())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.CATEGORY_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Given an existing category, When DELETE, Then 204 with no body")
    public void shouldDeleteCategory() throws Exception {
        given(categoryService.delete(ID)).willReturn(true);

        mockMvc.perform(delete(BASE + "/{id}", ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Given an unknown category, When DELETE, Then 404")
    public void shouldReturn404WhenDeletingUnknownCategory() throws Exception {
        given(categoryService.delete(ID)).willReturn(false);

        mockMvc.perform(delete(BASE + "/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.CATEGORY_NOT_FOUND.getCode()));
    }
}
