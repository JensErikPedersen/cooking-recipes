package dk.serik.recipes.service;

import org.springframework.http.HttpStatus;
import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.UnitDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mockutil.MockUnitUtil;
import dk.serik.recipes.model.Unit;
import dk.serik.recipes.repository.UnitJpaRepository;
import org.assertj.core.api.Assertions;
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
public class UnitServiceTest {
    @Mock
    private UnitJpaRepository repository;
    @Mock
    private Session session;
    @InjectMocks
    private UnitServiceImpl service;

    @Captor
    ArgumentCaptor<Unit> unitCaptor;

    @Test
    @DisplayName("Given existing Unit, When fetching Unit by Id, Then Unit is returned")
    public void shouldFetchUnitById() {
        final String unitId = "f7823293-7874-4459-9fb7-6b420a0627fa";
        // Given
        given(repository.findById(UUID.fromString(unitId))).willReturn(Optional.of(MockUnitUtil.mockGram()));

        // When
        Optional<UnitDTO> dto = service.findById(unitId);

        // Then
        then(repository).should(times(1)).findById(UUID.fromString(unitId));
        assertThat(dto.isPresent()).isTrue();
        assertThat(dto.get().getName()).isEqualTo("Gram");
    }

    @Test
    @DisplayName("Given fetch by unknown id, When Unit is fetched by Id, Then Empty is returned")
    public void shouldReturnEmptyWhenFetchedUnknownId() {
        final String unitId = "f7823293-7874-4459-9fb7-6b420a0627fa";
        // Given
        given(repository.findById(UUID.fromString(unitId))).willReturn(Optional.empty());

        // When
        Optional<UnitDTO> dto = service.findById(unitId);

        // Then
        then(repository).should(times(1)).findById(UUID.fromString(unitId));
        assertThat(dto.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Given two Units exist, When all is fetched, Then two is returned")
    public void shouldReturnFourUnits() {
        // Given
        given(repository.findAll()).willReturn(MockUnitUtil.mockAllUnits());

        // When
        List<UnitDTO> dtos = service.findAll();

        // Then
        then(repository).should(times(1)).findAll();
        assertThat(dtos.size()).isEqualTo(2);
        assertThat(dtos).containsExactlyElementsOf(MockUnitUtil.mockUnitDtos());
    }

    @Test
    @DisplayName("Given no Units, When fetch all, Then empty is returned")
    public void shouldReturnEmpty() {
        // Given
        given(repository.findAll()).willReturn(new ArrayList<>());

        // When
        List<UnitDTO> dtos = service.findAll();

        // Then
        then(repository).should(times(1)).findAll();
        assertThat(dtos).isEmpty();
    }

    @Test
    @DisplayName("Given valid Unit, When saved, Then Unit is returned")
    public void shouldSaveNewUnit() {
        // Given
        lenient().when(session.getUserName()).thenReturn("Jens");
        given(repository.save(any())).willReturn(MockUnitUtil.mockDl());

        // When
        UnitDTO savedDto = service.save(MockUnitUtil.mockUnitDlDTOToSave());

        // Then
        then(repository).should(times(1)).save(unitCaptor.capture());
        assertThat(savedDto).isNotNull();
        Assertions.assertThat(savedDto.getId()).isEqualTo("046d0928-0806-480b-ad8b-c6845e99643b");
        // createdBy must come from the session, not a hardcoded name
        Assertions.assertThat(unitCaptor.getValue().getCreatedBy()).isEqualTo("Jens");
    }

    @Test
    @DisplayName("Given Unit is null, When saved, Then a ServiceException is thrown")
    public void shouldThrowExceptionWhenSaved() {
        ServiceException e = assertThrows(ServiceException.class, () -> {
            service.save(null);
        }, "Unit is null");

        assertThat(e.getCode()).isEqualTo(ApplicationErrorCodes.UNIT_DTO_IS_NULL.getCode());
    }

    @Test
    @DisplayName("Given existing Unit, When updating label and name, Then Unit is updated and returned")
    public void shouldUpdatedUnitLabel() {
        // Given
        given(repository.findById(UUID.fromString("f7823293-7874-4459-9fb7-6b420a0627fa"))).willReturn(Optional.of(MockUnitUtil.mockGramToBeUpdated()));
        given(repository.save(any())).willReturn(MockUnitUtil.mockUpdatedGram());
        // When
        UnitDTO updated = service.update(MockUnitUtil.mockUnitGramDTOToBeUpdated());

        // Then
        then(repository).should(times(1)).findById(any());
        then(repository).should(times(1)).save(unitCaptor.capture());
        assertThat(updated).isNotNull();
        assertThat(unitCaptor.getValue().getLabel()).isEqualTo("gr");
        assertThat(unitCaptor.getValue().getName()).isEqualTo("Kilogram");
        assertThat(updated.getName()).isEqualTo("Kilogram");
        // an update must not overwrite the original creator
        assertThat(unitCaptor.getValue().getCreatedBy()).isEqualTo("Majken");
    }

    @Test
    @DisplayName("Given unit does not exist, When updating label and name, Then exception is thrown")
    public void shouldThrowExceptionWhenUpdatingNonExistingUnit() {
        // Given
        given(repository.findById(UUID.fromString("f7823293-7874-4459-9fb7-6b420a0627fa"))).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> {
            service.update(MockUnitUtil.mockUnitGramDTOToBeUpdated());
        }).isInstanceOf(ServiceException.class)
                .hasMessageContaining("Could not update Unit with id f7823293-7874-4459-9fb7-6b420a0627fa since it was not found");
    }

    @Test
    @DisplayName("Given existing Unit, When deleting unit, Then true is returned")
    public void shouldDeleteUnit() {
        // Given
        given(repository.findById(UUID.fromString("f7823293-7874-4459-9fb7-6b420a0627fa"))).willReturn(Optional.of(MockUnitUtil.mockGram()));

        // When
        boolean isDeleted = service.delete("f7823293-7874-4459-9fb7-6b420a0627fa");

        // Then
        then(repository).should(times(1)).findById(UUID.fromString("f7823293-7874-4459-9fb7-6b420a0627fa"));
        then(repository).should(times(1)).delete(MockUnitUtil.mockGram());
        assertThat(isDeleted).isTrue();
    }

    @Test
    @DisplayName("Given Unit not found, When deleted, Then false is returned")
    public void shouldReturnFalseWhenDeleteUnknown() {
        // Given
        given(repository.findById(UUID.fromString("381e5cd5-0a5d-48d2-b69c-71516254937e"))).willReturn(Optional.empty());

        // When
        boolean isDeleted = service.delete("381e5cd5-0a5d-48d2-b69c-71516254937e");

        // Then
        then(repository).should(times(1)).findById(UUID.fromString("381e5cd5-0a5d-48d2-b69c-71516254937e"));
        Assertions.assertThat(isDeleted).isFalse();
    }

    @Test
    @DisplayName("Given id is null, When deleted, Then service exception is thrown")
    public void shouldThrowExceptionWhenIdIsNull() {
        assertThrows(ServiceException.class, () -> {
            service.delete(null);
        }, "Unit Id is Null");
    }



    @Test
    @DisplayName("Given UnitDTO is null, When saving, Then a bad request is reported")
    public void shouldRejectSaveOfNullUnit() {
        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.UNIT_DTO_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given UnitDTO is null, When updating, Then a bad request is reported")
    public void shouldRejectUpdateOfNullUnit() {
        assertThatThrownBy(() -> service.update(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.UNIT_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given UnitDTO has no id, When updating, Then a bad request is reported")
    public void shouldRejectUpdateOfUnitWithoutId() {
        assertThatThrownBy(() -> service.update(UnitDTO.builder().build()))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.UNIT_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given id is null, When deleting, Then a bad request is reported")
    public void shouldRejectDeleteOfNullUnitId() {
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(ServiceException.class)
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.UNIT_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }


    @Test
    @DisplayName("Given a malformed id, When fetching a Unit by id, Then a bad request is reported")
    public void shouldRejectMalformedUnitIdOnFindById() {
        assertThatThrownBy(() -> service.findById("not-a-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.UNIT_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Given a malformed id, When deleting a Unit, Then a bad request is reported")
    public void shouldRejectMalformedUnitIdOnDelete() {
        assertThatThrownBy(() -> service.delete("not-a-uuid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("is not a valid UUID")
                .extracting("code", "httpStatus")
                .containsExactly(ApplicationErrorCodes.UNIT_ID_IS_NULL.getCode(), HttpStatus.BAD_REQUEST);
    }

}
