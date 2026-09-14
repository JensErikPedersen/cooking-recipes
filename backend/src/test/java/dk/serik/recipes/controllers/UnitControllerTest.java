package dk.serik.recipes.controllers;

import tools.jackson.databind.json.JsonMapper;
import dk.serik.recipes.dto.UnitDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mockutil.MockUnitUtil;
import dk.serik.recipes.service.UnitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
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

@WebMvcTest(UnitController.class)
public class UnitControllerTest {

    private static final String BASE = "/api/v1/units";
    private static final String ID = "f7823293-7874-4459-9fb7-6b420a0627fa";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private UnitService unitService;

    @Test
    @DisplayName("Given units exist, When GET the collection, Then 200 and the list is returned")
    public void shouldReturnAllUnits() throws Exception {
        given(unitService.findAll()).willReturn(MockUnitUtil.mockUnitDtos());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].label").value("dl"))
                .andExpect(jsonPath("$[1].name").value("Gram"));
    }

    @Test
    @DisplayName("Given no units, When GET the collection, Then 200 and an empty list")
    public void shouldReturnEmptyListWhenNoUnits() throws Exception {
        given(unitService.findAll()).willReturn(List.of());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Given an existing unit, When GET by id, Then 200 and the unit is returned")
    public void shouldReturnUnitById() throws Exception {
        given(unitService.findById(ID)).willReturn(Optional.of(MockUnitUtil.mockUnitGramDTO()));

        mockMvc.perform(get(BASE + "/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("g"))
                .andExpect(jsonPath("$.name").value("Gram"))
                .andExpect(jsonPath("$.id").value(ID));
    }

    @Test
    @DisplayName("Given an unknown unit, When GET by id, Then 404 and the error envelope")
    public void shouldReturn404WhenUnitUnknown() throws Exception {
        given(unitService.findById(ID)).willReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.UNIT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(containsString("could not be found")));
    }

    @Test
    @DisplayName("Given a malformed id, When GET by id, Then 400 from the service-layer id check")
    public void shouldReturn400ForMalformedId() throws Exception {
        given(unitService.findById("not-a-uuid"))
                .willThrow(ServiceException.badRequest(ApplicationErrorCodes.UNIT_ID_IS_NULL,
                        "Unit Id 'not-a-uuid' is not a valid UUID"));

        mockMvc.perform(get(BASE + "/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.UNIT_ID_IS_NULL.getCode()))
                .andExpect(jsonPath("$.message").value(containsString("not a valid UUID")));
    }

    @Test
    @DisplayName("Given a valid payload, When POST, Then 201 with a Location header")
    public void shouldCreateUnit() throws Exception {
        given(unitService.save(any())).willReturn(MockUnitUtil.mockUnitGramDTO());

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MockUnitUtil.mockUnitDlDTOToSave())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/" + ID)))
                .andExpect(jsonPath("$.name").value("Gram"));
    }

    @Test
    @DisplayName("Given a payload with no label, When POST, Then 400 and the service is never called")
    public void shouldRejectCreateWithoutLabel() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UnitDTO.builder().name("Gram").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()))
                .andExpect(jsonPath("$.validationExceptions[0].objectName").value("label"));

        verify(unitService, never()).save(any());
    }

    @Test
    @DisplayName("Given a label longer than the column allows, When POST, Then 400")
    public void shouldRejectCreateWithOversizedLabel() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UnitDTO.builder()
                                .label("x".repeat(26))   // column is VARCHAR(25)
                                .name("Gram")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationExceptions[0].objectName").value("label"));

        verify(unitService, never()).save(any());
    }

    @Test
    @DisplayName("Given a valid payload, When PUT, Then 200 and the path id wins over the body id")
    public void shouldUpdateUnitUsingPathId() throws Exception {
        given(unitService.update(any())).willReturn(MockUnitUtil.mockUnitGramDTOToBeUpdated());

        mockMvc.perform(put(BASE + "/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UnitDTO.builder()
                                .id("00000000-0000-0000-0000-000000000000")   // deliberately different
                                .label("gr")
                                .name("Kilogram")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kilogram"));

        ArgumentCaptor<UnitDTO> captor = ArgumentCaptor.forClass(UnitDTO.class);
        verify(unitService).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(ID);
    }

    @Test
    @DisplayName("Given an unknown unit, When PUT, Then the service's 404 is surfaced")
    public void shouldReturn404WhenUpdatingUnknownUnit() throws Exception {
        given(unitService.update(any())).willThrow(ServiceException.builder()
                .message("Could not update Unit with id " + ID + " since it was not found")
                .code(ApplicationErrorCodes.UNIT_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build());

        mockMvc.perform(put(BASE + "/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UnitDTO.builder().label("gr").name("Kilogram").build())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.UNIT_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Given an existing unit, When DELETE, Then 204 with no body")
    public void shouldDeleteUnit() throws Exception {
        given(unitService.delete(ID)).willReturn(true);

        mockMvc.perform(delete(BASE + "/{id}", ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Given an unknown unit, When DELETE, Then 404")
    public void shouldReturn404WhenDeletingUnknownUnit() throws Exception {
        given(unitService.delete(ID)).willReturn(false);

        mockMvc.perform(delete(BASE + "/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.UNIT_NOT_FOUND.getCode()));
    }
}
