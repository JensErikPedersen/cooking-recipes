package dk.serik.recipes.repository;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.model.Unit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@Slf4j
public class UnitJpaRepositoryIT {

	@Autowired
	private UnitJpaRepository repository;
	
	@MockitoBean
	private Session session;
	
	@BeforeEach
	public void setupTest() {
		Mockito.when(session.getUserName()).thenReturn("Jens");
	}
	
	@Test
	@DisplayName("Given four Units When Fetching All Then all Four Units are returned")
	public void shouldReturnAllFourUnits() {
		// When
		List<Unit> unitEntities = repository.findAll();

		//Then
		Assertions.assertFalse(unitEntities.isEmpty());
		Assertions.assertEquals(4, unitEntities.size());
	}

	@Test
	@DisplayName("Given existing Unit, When fetching unit by Id, Then Unit is returned")
	public void shouldReturnUnitById() {
		// Given - When
		Optional<Unit> unit = repository.findById(UUID.fromString("b9cef3df-4bb5-49ab-8bde-5848d1363bce"));

		// Then
		assertThat(unit.isPresent()).isTrue();
	}

	@Test
	@DisplayName("Given valid Unit When saved to database Then Unit is saved ok")
	public void shouldSaveNewUnit() {
		// Given
		OffsetDateTime now = OffsetDateTime.now();

		// When
		Unit saveAndFlush = repository.saveAndFlush(buildUnitEntity());

		// Then
		List<Unit> unitEntities = repository.findAll();
		Assertions.assertFalse(unitEntities.isEmpty());
		Assertions.assertEquals(5, unitEntities.size());
		assertThat(now).isCloseTo(saveAndFlush.getCreated(), within(0, ChronoUnit.SECONDS));
		assertThat("Jens").isEqualTo(saveAndFlush.getCreatedBy());		
		Assertions.assertEquals("Knivspids", saveAndFlush.getName());
	}

	@Test
	@DisplayName("Given existing Unit, When deleted by Id, Then Unit is removed from Database")
	public void shouldDeleteExistingUnit() {
		// Given
		Optional<Unit> unit = repository.findById(UUID.fromString("c5173731-3a7e-498c-84b1-b2d3abe68cef"));
		assertThat(unit.isPresent()).isTrue();

		// When
		if(unit.isPresent()) {
			repository.delete(unit.get());
		}

		// Then
		List<Unit> units = repository.findAll();
		assertThat(units).isNotEmpty();
		assertThat(units.size()).isEqualTo(3);
	}

	@Test
	@DisplayName("Given existing Unit, When updating label, Then label and updated info is updated")
	public void shouldUpdateUnit() {
		// Given
		Optional<Unit> unit = repository.findById(UUID.fromString("c5173731-3a7e-498c-84b1-b2d3abe68cef"));
		assertThat(unit.isPresent()).isTrue();

		// When
		unit.get().setLabel("Del");
		OffsetDateTime now = OffsetDateTime.now();
		Unit updatedUnit = repository.saveAndFlush(unit.get());
		assertThat(updatedUnit.getLabel()).isEqualTo("Del");
		assertThat(now).isCloseTo(updatedUnit.getUpdated(), within(0, ChronoUnit.SECONDS));
		assertThat(updatedUnit.getUpdatedBy()).isEqualTo("Jens");
		// the original creator must survive an update
		assertThat(updatedUnit.getCreatedBy()).isEqualTo("Majken");
	}

	private Unit buildUnitEntity() {
		Unit unit = new Unit();
		unit.setLabel("knvsps");
		unit.setName("Knivspids");
		return unit;
	}
}
