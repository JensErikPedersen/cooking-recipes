package dk.serik.recipes.controllers;

import tools.jackson.databind.json.JsonMapper;
import dk.serik.recipes.dto.IngredientDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mockutil.MockIngredientUtil;
import dk.serik.recipes.service.IngredientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

@WebMvcTest(IngredientController.class)
// The security chain is AuthenticationIT's business; these tests are about the controller.
@AutoConfigureMockMvc(addFilters = false)
public class IngredientControllerTest {

    private static final String BASE = "/api/v1/ingredients";
    private static final String ID = "01a50907-8141-4dd1-acdf-c4384669c2b2";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private IngredientService ingredientService;

    @Test
    @DisplayName("Given ingredients exist, When GET the collection, Then 200 and the list is returned")
    public void shouldReturnAllIngredients() throws Exception {
        given(ingredientService.findAll())
                .willReturn(List.of(MockIngredientUtil.mockHavsaltDTO(), MockIngredientUtil.mockHvedemelDTO()));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Havsalt"));
    }

    @Test
    @DisplayName("Given no ingredients, When GET the collection, Then 200 and an empty list")
    public void shouldReturnEmptyListWhenNoIngredients() throws Exception {
        given(ingredientService.findAll()).willReturn(List.of());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Given an existing ingredient, When GET by id, Then 200 and the ingredient is returned")
    public void shouldReturnIngredientById() throws Exception {
        given(ingredientService.findById(ID)).willReturn(Optional.of(MockIngredientUtil.mockHavsaltDTO()));

        mockMvc.perform(get(BASE + "/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Havsalt"))
                .andExpect(jsonPath("$.id").value(ID));
    }

    @Test
    @DisplayName("Given an unknown ingredient, When GET by id, Then 404 and the error envelope")
    public void shouldReturn404WhenIngredientUnknown() throws Exception {
        given(ingredientService.findById(ID)).willReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.INGREDIENT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(containsString("could not be found")));
    }

    @Test
    @DisplayName("Given a malformed id, When GET by id, Then 400 from the service-layer id check")
    public void shouldReturn400ForMalformedId() throws Exception {
        given(ingredientService.findById("not-a-uuid"))
                .willThrow(ServiceException.badRequest(ApplicationErrorCodes.INGREDIENT_ID_IS_NULL,
                        "Ingredient Id 'not-a-uuid' is not a valid UUID"));

        mockMvc.perform(get(BASE + "/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.INGREDIENT_ID_IS_NULL.getCode()))
                .andExpect(jsonPath("$.message").value(containsString("not a valid UUID")));
    }

    @Test
    @DisplayName("Given a valid payload, When POST, Then 201 with a Location header")
    public void shouldCreateIngredient() throws Exception {
        given(ingredientService.save(any())).willReturn(MockIngredientUtil.mockHavsaltDTO());

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MockIngredientUtil.mockNewHavsaltDTO())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/" + ID)))
                .andExpect(jsonPath("$.name").value("Havsalt"));
    }

    @Test
    @DisplayName("Given a payload with no name, When POST, Then 400 and the service is never called")
    public void shouldRejectCreateWithoutName() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                IngredientDTO.builder().description("no name supplied").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()))
                .andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"));

        verify(ingredientService, never()).save(any());
    }

    @Test
    @DisplayName("Given a valid payload, When PUT, Then 200 and the path id wins over the body id")
    public void shouldUpdateIngredientUsingPathId() throws Exception {
        given(ingredientService.update(any())).willReturn(MockIngredientUtil.mockHavsaltDTOToUpdate());

        mockMvc.perform(put(BASE + "/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(IngredientDTO.builder()
                                .id("00000000-0000-0000-0000-000000000000")   // deliberately different
                                .name("Havsalt")
                                .build())))
                .andExpect(status().isOk());

        ArgumentCaptor<IngredientDTO> captor = ArgumentCaptor.forClass(IngredientDTO.class);
        verify(ingredientService).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(ID);
    }

    @Test
    @DisplayName("Given an unknown ingredient, When PUT, Then the service's 404 is surfaced")
    public void shouldReturn404WhenUpdatingUnknownIngredient() throws Exception {
        given(ingredientService.update(any())).willThrow(ServiceException.builder()
                .message("Could not update Ingredient with id " + ID + " since it was not found")
                .code(ApplicationErrorCodes.INGREDIENT_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build());

        mockMvc.perform(put(BASE + "/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(IngredientDTO.builder().name("Havsalt").build())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.INGREDIENT_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Given an existing ingredient, When DELETE, Then 204 with no body")
    public void shouldDeleteIngredient() throws Exception {
        given(ingredientService.delete(ID)).willReturn(true);

        mockMvc.perform(delete(BASE + "/{id}", ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Given an unknown ingredient, When DELETE, Then 404")
    public void shouldReturn404WhenDeletingUnknownIngredient() throws Exception {
        given(ingredientService.delete(ID)).willReturn(false);

        mockMvc.perform(delete(BASE + "/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.INGREDIENT_NOT_FOUND.getCode()));
    }
}
