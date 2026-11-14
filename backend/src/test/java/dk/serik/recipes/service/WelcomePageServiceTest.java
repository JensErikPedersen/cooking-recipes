package dk.serik.recipes.service;

import dk.serik.recipes.dto.PictureDTO;
import dk.serik.recipes.dto.WelcomePageDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.model.Picture;
import dk.serik.recipes.model.WelcomePage;
import dk.serik.recipes.repository.PictureJpaRepository;
import dk.serik.recipes.repository.WelcomePageJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WelcomePageServiceTest {

    private static final UUID PICTURE_ID = UUID.fromString("0f4f78b9-b861-40a5-b4be-fa95c70ec50c");

    @Mock
    private WelcomePageJpaRepository repository;

    @Mock
    private PictureJpaRepository pictureRepository;

    @InjectMocks
    private WelcomePageServiceImpl service;

    private final WelcomePage row = new WelcomePage();

    @BeforeEach
    void setUp() {
        when(repository.findAll()).thenReturn(List.of(row));
        lenient().when(repository.save(row)).thenReturn(row);
    }

    @Test
    @DisplayName("Given no picture, When read, Then a null picture")
    void shouldHaveNoPicture() {
        assertThat(service.get().picture()).isNull();
    }

    @Test
    @DisplayName("Given a picture's id, When updated, Then the page shows that picture")
    void shouldSetThePicture() {
        when(pictureRepository.findById(PICTURE_ID)).thenReturn(Optional.of(picture()));

        WelcomePageDTO updated = service.update(new WelcomePageDTO(PictureDTO.builder().id(PICTURE_ID.toString()).build()));

        assertThat(row.getPicture().getId()).isEqualTo(PICTURE_ID);
        assertThat(updated.picture().getName()).isEqualTo("Rugbrød");
    }

    @Test
    @DisplayName("Given a page with a picture, When updated with none, Then the picture is removed")
    void shouldRemoveThePicture() {
        row.setPicture(picture());

        assertThat(service.update(new WelcomePageDTO(null)).picture()).isNull();
        assertThat(row.getPicture()).isNull();
    }

    @Test
    @DisplayName("Given an unknown picture, When updated, Then 400 on the field picture, and nothing is saved")
    void shouldRefuseAnUnknownPicture() {
        when(pictureRepository.findById(PICTURE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(new WelcomePageDTO(PictureDTO.builder().id(PICTURE_ID.toString()).build())))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException e = (ServiceException) ex;
                    assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo(ApplicationErrorCodes.PICTURE_NOT_FOUND.getCode());
                    assertThat(e.getFieldErrors()).extracting("objectName", "message")
                            .containsExactly(tuple("picture", "This picture does not exist"));
                });
        verify(repository, never()).save(any());
    }

    private static Picture picture() {
        Picture picture = new Picture();
        picture.setName("Rugbrød");
        picture.setAltText("Et rugbrød");
        ReflectionTestUtils.setField(picture, "id", PICTURE_ID);
        return picture;
    }
}
