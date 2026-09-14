package dk.serik.recipes.service;

import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.RatingDTO;
import dk.serik.recipes.exceptions.ServiceException;
import org.springframework.http.HttpStatus;
import dk.serik.recipes.mockutil.MockRatingUtil;
import dk.serik.recipes.model.Rating;
import dk.serik.recipes.repository.RatingJpaRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class RatingServiceTest {
    @Mock
    private RatingJpaRepository repository;

    @Mock
    private Session session;

    @InjectMocks
    private RatingServiceImpl service;

    @Captor
    ArgumentCaptor<Rating> ratingCaptor;

    @BeforeEach
    public void setupTest() {
        lenient().when(session.getUserName()).thenReturn("Jens");
    }

    @Test
    @DisplayName("Given existing rating, When fetching by Id, Then rating is returned")
    public void shouldReturnRatingById() {
        // Given
        given(repository.findById(MockRatingUtil.mockRating5().getId())).willReturn(Optional.of(MockRatingUtil.mockRating5()));

        // When
        Optional<RatingDTO> optional = service.findById(MockRatingUtil.mockRatingDTO5().getId());

        // Then
        then(repository).should(times(1)).findById(MockRatingUtil.mockRating5().getId());
        assertThat(optional.isPresent()).isTrue();
        assertThat(optional.get().getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("Given existing rating, When fetching by unknown Id, Then empty is returned")
    public void shouldReturnEmptyWhenUnknownId() {
        // Given
        given(repository.findById(MockRatingUtil.mockRating5().getId())).willReturn(Optional.empty());

        // When
        Optional<RatingDTO> optional = service.findById(MockRatingUtil.mockRatingDTO5().getId());

        // Then
        then(repository).should(times(1)).findById(MockRatingUtil.mockRating5().getId());
        assertThat(optional.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Given two ratings, When fetching all, Then two is returned")
    public void shouldReturnTwoRatings() {
        // Given
        given(repository.findAll()).willReturn(MockRatingUtil.mockAllRatings());

        // When
        List<RatingDTO> allRatings = service.findAll();

        // Then
        then(repository).should(times(1)).findAll();
        assertThat(allRatings.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given no ratings, When fetching all, Then empty is returned")
    public void shouldReturnEmpty() {
        // Given
        given(repository.findAll()).willReturn(new ArrayList<>());

        // When
        List<RatingDTO> allRatings = service.findAll();

        // Then
        then(repository).should(times(1)).findAll();
        assertThat(allRatings).isEmpty();
    }

    @Test
    @DisplayName("Given valid RatingDTO, When saving Rating, Then new Rating is returned")
    public void shouldSaveNewRating() {
        // Given
        given(repository.save(any())).willReturn(MockRatingUtil.mockRating5());

        // When
        RatingDTO dto = service.save(MockRatingUtil.mockRatingDTO5());

        // Then
        then(repository).should(times(1)).save(ratingCaptor.capture());
        assertThat(dto.getRating()).isEqualTo(5);
        assertThat(ratingCaptor.getValue().getRating()).isEqualTo(5);
        assertThat(ratingCaptor.getValue().getDescription()).isEqualTo("Outstanding");
    }

    @Test
    @DisplayName("Given ratingdto is null, When saving, Then exception is thrown")
    public void shouldThrowExceptionWhenRatingDTOIsNull() {
       assertThatThrownBy(() -> {
           service.save(null);
       }).isInstanceOf(ServiceException.class)
               .hasMessageContaining("Rating is null")
               .extracting("httpStatus")
               .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given existing rating, When deleted, Then return true")
    public void shouldDeleteAndReturnTrue() {
        // Given
        given(repository.findById(MockRatingUtil.mockRating5().getId())).willReturn(Optional.of(MockRatingUtil.mockRating5()));

        // When
        boolean isDeleted = service.delete(String.valueOf(MockRatingUtil.mockRating5().getId()));

        // Then
        then(repository).should(times(1)).findById(any());
        then(repository).should(times(1)).delete(any());
        assertThat(isDeleted).isTrue();
    }

    @Test
    @DisplayName("Given rating not found, When deleted, Then false is returned")
    public void shouldReturnFalseIfRatingNotFound() {
        // Given
        given(repository.findById(UUID.fromString("7c89ec02-63b9-4d68-9720-c22396fca1c7"))).willReturn(Optional.empty());

        // When
        boolean isDeleted = service.delete("7c89ec02-63b9-4d68-9720-c22396fca1c7");

        // Then
        then(repository).should(times(1)).findById(any());
        then(repository).shouldHaveNoMoreInteractions();
        assertThat(isDeleted).isFalse();
    }

    @Test
    @DisplayName("Given id is null, When rating is deleted, Then exception is thrown")
    public void shouldThrowExceptionWhenDeletingByIdNull() {
        assertThatThrownBy(() -> {
            service.delete(null);
        }).isInstanceOf(ServiceException.class)
                .hasMessageContaining("Rating Id is null")
                .extracting("code", "httpStatus")
                .containsExactly(501, HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given valid rating dto, When updating description, Then rating is updated")
    public void shouldUpdateRatingDescription() {
        // Given
        given(repository.findById(MockRatingUtil.mockRating5().getId())).willReturn(Optional.of(MockRatingUtil.mockRating5()));
        given(repository.save(any())).willReturn(MockRatingUtil.mockRating5Updated());

        // When
        RatingDTO updated = service.update(MockRatingUtil.mockRatingDTO5ToBeUpdated());

        // Then
        then(repository).should(times(1)).findById(MockRatingUtil.mockRating5().getId());
        then(repository).should(times(1)).save(ratingCaptor.capture());
        assertThat(updated).isNotNull();
        assertThat(ratingCaptor.getValue().getDescription()).isEqualTo("Fantastic");
        assertThat(ratingCaptor.getValue().getId()).isEqualTo(MockRatingUtil.mockRating5().getId());
        assertThat(updated.getDescription()).isEqualTo("Fantastic");
    }

    @Test
    @DisplayName("Given rating do not exist, When updating description, Then throw exception")
    public void shouldThrowExceptionWhenUpdatingNonExistingRating() {
        // Given
        given(repository.findById(MockRatingUtil.mockRating5().getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> {
            service.update(MockRatingUtil.mockRatingDTO5());
        }).isInstanceOf(ServiceException.class)
                .hasMessageContaining("Could not update Rating with id 7c89ec02-63b9-4d68-9720-c22396fca1c7 since it was not found")
                .extracting("httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);


    }

    @Test
    @DisplayName("Given RatingDTO is null, When saving, Then a bad request is reported")
    public void shouldRejectSaveOfNullRating() {
        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RATING_DTO_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given RatingDTO is null, When updating, Then a bad request is reported")
    public void shouldRejectUpdateOfNullRating() {
        assertThatThrownBy(() -> service.update(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RATING_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given RatingDTO has no id, When updating, Then a bad request is reported")
    public void shouldRejectUpdateOfRatingWithoutId() {
        assertThatThrownBy(() -> service.update(RatingDTO.builder().build()))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RATING_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given id is null, When deleting, Then a bad request is reported")
    public void shouldRejectDeleteOfNullRatingId() {
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RATING_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }


    @Test
    @DisplayName("Given a malformed id, When fetching a Rating by id, Then a bad request is reported")
    public void shouldRejectMalformedRatingIdOnFindById() {
        assertThatThrownBy(() -> service.findById("not-a-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RATING_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given a malformed id, When deleting a Rating, Then a bad request is reported")
    public void shouldRejectMalformedRatingIdOnDelete() {
        assertThatThrownBy(() -> service.delete("not-a-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.RATING_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

}
