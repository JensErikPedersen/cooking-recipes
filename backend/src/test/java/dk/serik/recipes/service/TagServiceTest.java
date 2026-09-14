package dk.serik.recipes.service;

import org.springframework.http.HttpStatus;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.TagDTO;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mockutil.MockTagUtil;
import dk.serik.recipes.model.Tag;
import dk.serik.recipes.repository.TagJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class TagServiceTest {
    @Mock
    private TagJpaRepository repository;
    @Mock
    private Session session;
    @InjectMocks
    private TagServiceImpl service;

    @Captor
    ArgumentCaptor<Tag> tagCaptor;

    @BeforeEach
    public void setupTest() {
        lenient().when(session.getUserName()).thenReturn("Jens");
    }

    @Test
    @DisplayName("Context is checked ok")
    public void contextIsOk() {
        assertThat(service).isNotNull();
        assertThat(repository).isNotNull();
    }
    @Test
    @DisplayName("Given existing Tag, When fetching by Id, Then Tag is returned")
    public void shouldFetchExistingTagById() {
        // Given
        given(repository.findById(UUID.fromString("1dbdaf66-0e50-4a7f-868e-2689da2bc32a"))).willReturn(Optional.of(MockTagUtil.mockTagSweet()));

        // When
        Optional<TagDTO> optional = service.findById("1dbdaf66-0e50-4a7f-868e-2689da2bc32a");

        // Then
        then(repository).should(times(1)).findById(UUID.fromString("1dbdaf66-0e50-4a7f-868e-2689da2bc32a"));
        assertThat(optional.isPresent()).isTrue();
        assertThat(optional.get().getName()).isEqualTo("Sødt");
    }

    @Test
    @DisplayName("Given Tag do noy exist by Id, When fetching by Id, Then empty is returned")
    public void shouldReturnEmptyWhenNoneExistById() {
        // Given
        given(repository.findById(UUID.fromString("1dbdaf66-0e50-4a7f-868e-2689da2bc32a"))).willReturn(Optional.empty());

        // When
        Optional<TagDTO> optional = service.findById("1dbdaf66-0e50-4a7f-868e-2689da2bc32a");

        // Then
        then(repository).should(times(1)).findById(UUID.fromString("1dbdaf66-0e50-4a7f-868e-2689da2bc32a"));
        assertThat(optional.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Given id is null, When fetching by id is null, Then throw exception")
    public void shouldThrowExceptionWhenFetchingById() {
        assertThatThrownBy(() -> {
            service.findById(null);
        }).isInstanceOf(ServiceException.class)
                .hasMessageContaining("Tag Id is null");
    }

    @Test
    @DisplayName("Given existing Tag, When fetching by invalid Id, Then a bad request is reported")
    public void shouldReturnExceptionWhenFetchingByInvalidId() {
        // a malformed id is a client error, not a raw IllegalArgumentException escaping the service
        assertThatThrownBy(() -> service.findById("not-valid-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("httpStatus")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given two Tags, When fetching all, Then two is returned")
    public void shouldReturnTwoTags() {
        // Given
        given(repository.findAll()).willReturn(MockTagUtil.mockTagList());

        //When
        List<TagDTO> dtos = service.findAll();

        // Then
        then(repository).should(times(1)).findAll();
        assertThat(dtos.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given Tag do noy exist by Id, When fetching by Id, Then empty is returned")
    public void shouldReturnEmptyWhenNoneExist() {
        // Given
        given(repository.findAll()).willReturn(new ArrayList<>());

        // When
        List<TagDTO> optional = service.findAll();

        // Then
        then(repository).should(times(1)).findAll();
        assertThat(optional.isEmpty()).isTrue();
    }


    @Test
    @DisplayName("Given valid TagDTO, When saving, Then Tag is saved")
    public void shouldSaveValidTag() {
        // Given
        given(repository.save(any())).willReturn(MockTagUtil.mockTagMexi());

        // When
        TagDTO savedDto = service.save(MockTagUtil.mockMexiTagDTO());

        // Then
        then(repository).should(times(1)).save(any());
        assertThat(savedDto).isNotNull();
        assertThat(savedDto.getId()).isEqualTo("0f569775-68aa-44c1-94b5-1c694dec8890");
        assertThat(savedDto.getCreatedBy()).isEqualTo("Jens");
    }

    @Test
    @DisplayName("Given Tag Mexi, When fetching by name Mexi, Then Tag Mexi is returned")
    public void shouldReturnTagMexiWhenFetchingByName() {
        // Given
        given(repository.findTagByName("Mexi")).willReturn(Optional.of(MockTagUtil.mockTagMexi()));

        // When
        Optional<TagDTO> optional = service.findTagByName("Mexi");

        // Then
        then(repository).should(times(1)).findTagByName("Mexi");
        assertThat(optional.isPresent()).isTrue();
        assertThat(optional.get().getName()).isEqualTo("Mexi");
    }

    @Test
    @DisplayName("Given Unknown Tag Mexi, When fetching by name Mexi, Then empty is returned")
    public void shouldReturnEmptyWhenFetchingByUnknownName() {
        // Given
        given(repository.findTagByName("Mexi")).willReturn(Optional.empty());

        // When
        Optional<TagDTO> optional = service.findTagByName("Mexi");

        // Then
        then(repository).should(times(1)).findTagByName("Mexi");
        assertThat(optional.isPresent()).isFalse();
    }

    @Test
    @DisplayName("Given existing Tag, When deleting by Id, Then true is returned")
    public void shouldDeleteTag() {
        // Given
        given(repository.findById(UUID.fromString("0f569775-68aa-44c1-94b5-1c694dec8890"))).willReturn(Optional.of(MockTagUtil.mockTagMexi()));

        // When
        boolean isDeleted = service.delete("0f569775-68aa-44c1-94b5-1c694dec8890");

        // Then
        then(repository).should(times(1)).findById(UUID.fromString("0f569775-68aa-44c1-94b5-1c694dec8890"));
        then(repository).should(times(1)).delete(any());
        assertThat(isDeleted).isTrue();
    }

    @Test
    @DisplayName("Given existing Tag, When deleting by unknown Id, Then false is returned")
    public void shouldReturnFalseWhenDeleteUnknownId() {
        // Given
        given(repository.findById(UUID.fromString("0f569775-68aa-44c1-94b5-1c694dec8890"))).willReturn(Optional.empty());

        // When
        boolean isDeleted = service.delete("0f569775-68aa-44c1-94b5-1c694dec8890");

        // Then
        then(repository).should(times(1)).findById(UUID.fromString("0f569775-68aa-44c1-94b5-1c694dec8890"));
        then(repository).shouldHaveNoMoreInteractions();
        assertThat(isDeleted).isFalse();
    }

    @Test
    @DisplayName("Given existing Tag, When deleting by Id null, Then exception is thrown")
    public void shouldThrowExceptionWhenDeleteByNull() {
        assertThatThrownBy(() -> {
           service.delete(null);
        }).isInstanceOf(ServiceException.class)
                .hasMessageContaining("Tag Id is null");
    }

    @Test
    @DisplayName("Given valid TagDTO, When updating Tag name, Then Tag name is updated")
    public void shouldUpdateTagName() {
        // Given
        given(repository.findById(UUID.fromString("0f569775-68aa-44c1-94b5-1c694dec8890"))).willReturn(Optional.of(MockTagUtil.mockTagMexi()));
        given(repository.save(any())).willReturn(MockTagUtil.mockTagMexiUpdated());
        // When
        TagDTO updated = service.update(MockTagUtil.mockMexiTagDTOToBeUpdated());

        // Then
        then(repository).should(times(1)).findById(UUID.fromString("0f569775-68aa-44c1-94b5-1c694dec8890"));
        assertThat(updated).isNotNull();
        then(repository).should(times(1)).save(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getName()).isEqualTo(MockTagUtil.mockMexiTagDTOToBeUpdated().getName());
        assertThat(tagCaptor.getValue().getId().toString()).isEqualTo(MockTagUtil.mockMexiTagDTOToBeUpdated().getId());
        assertThat(updated.getName()).isEqualTo("Meximad");
    }

    @Test
    @DisplayName("Given unknown Tag, When update Tag name, Then xception is thrown")
    public void shouldThrowExceptionWhenUpdatingUnknownTag() {
        // Given
        given(repository.findById(UUID.fromString("0f569775-68aa-44c1-94b5-1c694dec8890"))).willReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> {
            service.update(MockTagUtil.mockMexiTagDTO());
        }).isInstanceOf(ServiceException.class)
                .hasMessageContaining("Could not update Tag with id 0f569775-68aa-44c1-94b5-1c694dec8890 since it was not found");
    }


    @Test
    @DisplayName("Given TagDTO is null, When saving, Then a bad request is reported")
    public void shouldRejectSaveOfNullTag() {
        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.TAG_DTO_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given TagDTO is null, When updating, Then a bad request is reported")
    public void shouldRejectUpdateOfNullTag() {
        assertThatThrownBy(() -> service.update(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.TAG_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given TagDTO has no id, When updating, Then a bad request is reported")
    public void shouldRejectUpdateOfTagWithoutId() {
        assertThatThrownBy(() -> service.update(TagDTO.builder().build()))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.TAG_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given id is null, When deleting, Then a bad request is reported")
    public void shouldRejectDeleteOfNullTagId() {
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.TAG_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }


    @Test
    @DisplayName("Given a malformed id, When fetching a Tag by id, Then a bad request is reported")
    public void shouldRejectMalformedTagIdOnFindById() {
        assertThatThrownBy(() -> service.findById("not-a-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.TAG_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given a malformed id, When deleting a Tag, Then a bad request is reported")
    public void shouldRejectMalformedTagIdOnDelete() {
        assertThatThrownBy(() -> service.delete("not-a-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.TAG_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

}
