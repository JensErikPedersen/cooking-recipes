package dk.serik.recipes.mockutil;

import dk.serik.recipes.dto.UnitDTO;
import dk.serik.recipes.model.Unit;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MockUnitUtil {

    public static Unit mockGram() {
        Unit mock = Unit.builder()
                .name("Gram")
                        .label("g")
                .build();
        mock.setCreatedBy("Majken");
        mock.setUpdatedBy("Jens");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("f7823293-7874-4459-9fb7-6b420a0627fa"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }

    public static Unit mockGramToBeUpdated() {
        Unit mock = new Unit();
        mock.setName("Gram");
        mock.setLabel("g");
        mock.setCreatedBy("Majken");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("f7823293-7874-4459-9fb7-6b420a0627fa"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }

    public static Unit mockUpdatedGram() {
        Unit mock = new Unit();
        mock.setName("Kilogram");
        mock.setLabel("gr");
        mock.setCreatedBy("Majken");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("f7823293-7874-4459-9fb7-6b420a0627fa"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }

    public static Unit mockDl() {
        Unit mock = new Unit();
        mock.setName("Deciliter");
        mock.setLabel("dl");
        mock.setCreatedBy("Majken");
        mock.setUpdatedBy("Jens");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("046d0928-0806-480b-ad8b-c6845e99643b"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }

    public static List<Unit> mockAllUnits() {
        return Arrays.asList(mockDl(), mockGram());
    }

    public static UnitDTO mockUnitDeciliterDTO() {
        UnitDTO mock = UnitDTO.builder()
                .id("046d0928-0806-480b-ad8b-c6845e99643b")
                .createdBy("Majken")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"))
                .updatedBy("Jens")
                .updated(OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"))
                .label("dl")
                .name("Deciliter")
                .build();
        return mock;
    }

    public static UnitDTO mockUnitGramDTO() {
        UnitDTO mock = UnitDTO.builder()
                .id("f7823293-7874-4459-9fb7-6b420a0627fa")
                .createdBy("Majken")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"))
                .updatedBy("Jens")
                .updated(OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"))
                .label("g")
                .name("Gram")
                .build();
        return mock;
    }

    public static UnitDTO mockUnitGramDTOToBeUpdated() {
        UnitDTO mock = UnitDTO.builder()
                .id("f7823293-7874-4459-9fb7-6b420a0627fa")
                .createdBy("Majken")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"))
                .label("gr")
                .name("Kilogram")
                .build();
        return mock;
    }

    public static List<UnitDTO> mockUnitDtos() {
        return Arrays.asList(mockUnitDeciliterDTO(), mockUnitGramDTO());
    }

    public static Unit mockDlToSave() {
        Unit mock = new Unit();
        mock.setName("Deciliter");
        mock.setLabel("dl");
        mock.setCreatedBy("Majken");
        return mock;
    }

    public static UnitDTO mockUnitDlDTOToSave() {
        UnitDTO mock = UnitDTO.builder()
                .createdBy("Majken")
                .label("dl")
                .name("Deciliter")
                .build();
        return mock;
    }
}
