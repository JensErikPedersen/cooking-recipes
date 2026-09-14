package dk.serik.recipes.repository;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.model.Rating;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Slf4j
public class RatingJpaRepositotyIT {
	
	@Autowired
	private RatingJpaRepository ratingJpaRepository;
	
	@MockitoBean
	private Session session;	
	
	@BeforeEach
	public void initTest() {
		Mockito.when(session.getUserName()).thenReturn("Majken");
	}
	
	@Test
	public void givenRatingsExisting_WhenFetchingOneById_ThenOk() {
		Optional<Rating> opRating = ratingJpaRepository.findById(UUID.fromString("62149cc5-73e4-45ef-8ac2-4d725815d5cf"));
		assertThat(opRating.isPresent()).isTrue();
		assertThat(opRating.get().getRating()).isEqualTo(4);
	}
	
	@Test
	public void givenValidRatingEntity_WhenSaved_ThenOk() {
		OffsetDateTime now = OffsetDateTime.now();
		Rating rating = ratingJpaRepository.saveAndFlush(buildRatingEntity());
		assertThat(rating.getCreated()).isCloseTo(now, within(0, ChronoUnit.SECONDS));
		assertThat(rating.getCreatedBy()).isEqualTo("Majken");
	}
	
	
	@Test
	public void givenInValidRatingEntity_WhenSavedAndExceptionIsThrown_ThenOk() {		
		Rating newRating = buildRatingEntity();
		newRating.setRating(5);  // already exist
		Exception exception = assertThrows(DataIntegrityViolationException.class, () -> {
			ratingJpaRepository.saveAndFlush(newRating);
		});	
		
		log.info("Exception: {}", exception.getMessage());
		assertThat(exception).hasCauseInstanceOf(ConstraintViolationException.class);
	}
	
	@Test
	public void givenExistingRatingEntity_WhenDeleted_ThenOk() {
		Optional<Rating> opRating = ratingJpaRepository.findById(UUID.fromString("62149cc5-73e4-45ef-8ac2-4d725815d5cf"));
		assertThat(opRating.isPresent()).isTrue();
		
		ratingJpaRepository.delete(opRating.get());
		List<Rating> ratingEntities = ratingJpaRepository.findAll();
		assertThat(ratingEntities).isNotEmpty();
		assertEquals(4, ratingEntities.size());
	}
	
	
	@Test
	public void givenExistingRatingEntity_WhenUpdated_ThenOk() {
		Optional<Rating> opRating = ratingJpaRepository.findById(UUID.fromString("62149cc5-73e4-45ef-8ac2-4d725815d5cf"));
		assertThat(opRating.isPresent()).isTrue();		
		opRating.get().setDescription("Magnifique!");
		OffsetDateTime now = OffsetDateTime.now();
		Rating saveAndFlush = ratingJpaRepository.saveAndFlush(opRating.get());
		assertThat(saveAndFlush.getUpdated()).isCloseTo(now, within(0, ChronoUnit.SECONDS));
		assertThat(saveAndFlush.getUpdatedBy()).isEqualTo("Majken");
		assertThat(saveAndFlush.getDescription()).isEqualTo("Magnifique!");
	}
	
	private Rating buildRatingEntity() {
		Rating rating = new Rating();
		rating.setDescription("Helt fænomenal... verdensklasse!!!");
		rating.setRating(6);
		return rating;
		
	}

}
