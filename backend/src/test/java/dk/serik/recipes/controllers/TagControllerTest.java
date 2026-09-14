package dk.serik.recipes.controllers;

import tools.jackson.databind.json.JsonMapper;
import dk.serik.recipes.dto.TagDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mockutil.MockTagUtil;
import dk.serik.recipes.service.TagService;
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

@WebMvcTest(TagController.class)
public class TagControllerTest {

    private static final String BASE = "/api/v1/tags";
    private static final String ID = "0f569775-68aa-44c1-94b5-1c694dec8890";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private TagService tagService;

    @Test
    @DisplayName("Given tags exist, When GET the collection, Then 200 and the list is returned")
    public void shouldReturnAllTags() throws Exception {
        given(tagService.findAll())
                .willReturn(List.of(MockTagUtil.mockMexiTagDTO(), MockTagUtil.mockSweetTagDTO()));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Mexi"))
                .andExpect(jsonPath("$[1].name").value("Sødt"));
    }

    @Test
    @DisplayName("Given no tags, When GET the collection, Then 200 and an empty list")
    public void shouldReturnEmptyListWhenNoTags() throws Exception {
        given(tagService.findAll()).willReturn(List.of());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Given an existing tag, When GET by id, Then 200 and the tag is returned")
    public void shouldReturnTagById() throws Exception {
        given(tagService.findById(ID)).willReturn(Optional.of(MockTagUtil.mockMexiTagDTO()));

        mockMvc.perform(get(BASE + "/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mexi"))
                .andExpect(jsonPath("$.id").value(ID));
    }

    @Test
    @DisplayName("Given an unknown tag, When GET by id, Then 404 and the error envelope")
    public void shouldReturn404WhenTagUnknown() throws Exception {
        given(tagService.findById(ID)).willReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.TAG_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(containsString("could not be found")));
    }

    @Test
    @DisplayName("Given a malformed id, When GET by id, Then 400 from the service-layer id check")
    public void shouldReturn400ForMalformedId() throws Exception {
        given(tagService.findById("not-a-uuid"))
                .willThrow(ServiceException.badRequest(ApplicationErrorCodes.TAG_ID_IS_NULL,
                        "Tag Id 'not-a-uuid' is not a valid UUID"));

        mockMvc.perform(get(BASE + "/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.TAG_ID_IS_NULL.getCode()))
                .andExpect(jsonPath("$.message").value(containsString("not a valid UUID")));
    }

    @Test
    @DisplayName("Given a valid payload, When POST, Then 201 with a Location header")
    public void shouldCreateTag() throws Exception {
        given(tagService.save(any())).willReturn(MockTagUtil.mockMexiTagDTO());

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TagDTO.builder().name("Mexi").build())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/" + ID)))
                .andExpect(jsonPath("$.name").value("Mexi"));
    }

    @Test
    @DisplayName("Given a payload with a blank name, When POST, Then 400 and the service is never called")
    public void shouldRejectCreateWithBlankName() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TagDTO.builder().name("   ").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()))
                .andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"));

        verify(tagService, never()).save(any());
    }

    @Test
    @DisplayName("Given a valid payload, When PUT, Then 200 and the path id wins over the body id")
    public void shouldUpdateTagUsingPathId() throws Exception {
        given(tagService.update(any())).willReturn(MockTagUtil.mockMexiTagDTOToBeUpdated());

        mockMvc.perform(put(BASE + "/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TagDTO.builder()
                                .id("00000000-0000-0000-0000-000000000000")   // deliberately different
                                .name("Meximad")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Meximad"));

        ArgumentCaptor<TagDTO> captor = ArgumentCaptor.forClass(TagDTO.class);
        verify(tagService).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(ID);
    }

    @Test
    @DisplayName("Given an unknown tag, When PUT, Then the service's 404 is surfaced")
    public void shouldReturn404WhenUpdatingUnknownTag() throws Exception {
        given(tagService.update(any())).willThrow(ServiceException.builder()
                .message("Could not update Tag with id " + ID + " since it was not found")
                .code(ApplicationErrorCodes.TAG_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build());

        mockMvc.perform(put(BASE + "/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TagDTO.builder().name("Meximad").build())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.TAG_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Given an existing tag, When DELETE, Then 204 with no body")
    public void shouldDeleteTag() throws Exception {
        given(tagService.delete(ID)).willReturn(true);

        mockMvc.perform(delete(BASE + "/{id}", ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Given an unknown tag, When DELETE, Then 404")
    public void shouldReturn404WhenDeletingUnknownTag() throws Exception {
        given(tagService.delete(ID)).willReturn(false);

        mockMvc.perform(delete(BASE + "/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.TAG_NOT_FOUND.getCode()));
    }
}
