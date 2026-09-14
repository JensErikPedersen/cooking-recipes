package dk.serik.recipes.mockutil;

import dk.serik.recipes.dto.IngredientDTO;
import dk.serik.recipes.model.Ingredient;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

public class MockIngredientUtil {

    public static Ingredient mockHvedemel() {
        Ingredient mock = new Ingredient();
        mock.setName("Hvedemel");
        mock.setDescription("Sigtet hvedemel uden skalder og kim");
        mock.setCreatedBy("Majken");
        mock.setUpdatedBy("Jens");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("5f01d434-5a68-4359-9f2e-0a6793dce48d"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }

    public static Ingredient mockRugmel() {
        Ingredient mock = new Ingredient();
        mock.setName("Rugmel");
        mock.setDescription("Groft rugmel med skalder og kim");
        mock.setCreatedBy("Majken");
        mock.setUpdatedBy("Jens");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("3aebe786-f8fd-4d2e-92f5-cfe1669f9aa4"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2025-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2024-11-05T19:47:29"));
        return mock;
    }


    public static IngredientDTO mockHvedemelDTO() {
        IngredientDTO mock = IngredientDTO.builder()
                .id("5f01d434-5a68-4359-9f2e-0a6793dce48d")
                .name("Hvedemel")
                .description("Sigtet hvedemel uden skalder og kim")
                .createdBy("Majken")
                .updatedBy("Jens")
                .updated(OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"))
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"))
                .build();
        return mock;
    }

    public static Ingredient mockHavsalt() {
        Ingredient mock = new Ingredient();
        mock.setName("Havsalt");
        mock.setDescription("Havsalt fra middelhavet");
        mock.setCreatedBy("Jens");
        mock.setUpdatedBy("Jens");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("01a50907-8141-4dd1-acdf-c4384669c2b2"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-04-05T19:47:29"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-05-25T14:25:15"));
        return mock;
    }

    public static Ingredient mockSurdej() {
        Ingredient mock = new Ingredient();
        mock.setName("Surdej");
        mock.setDescription("Tynd surdej til hvedebrød");
        mock.setCreatedBy("Jens");
        mock.setUpdatedBy("Majken");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("1d150b3f-1a7c-4c08-8264-2910af5b9d25"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-04-05T19:47:29"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-05-25T14:25:15"));
        return mock;
    }

    public static Ingredient mockHavsaltUpdated() {
        Ingredient mock = new Ingredient();
        mock.setName("Havsalt");
        mock.setDescription("Havsalt fra middelhavet i store flager");
        mock.setCreatedBy("Jens");
        mock.setUpdatedBy("Jens");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("01a50907-8141-4dd1-acdf-c4384669c2b4"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-04-05T19:47:29"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-05-25T14:25:15"));
        return mock;
    }

    public static IngredientDTO mockHavsaltDTO() {
        return IngredientDTO.builder()
                .id("01a50907-8141-4dd1-acdf-c4384669c2b2")
                .name("Havsalt")
                .description("Havsalt fra middelhavet")
                .createdBy("Jens")
                .updatedBy("Jens")
                .updated(OffsetDateTimeProvider.provideIsoFullFormat("2023-05-25T14:25:15"))
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-04-05T19:47:29"))
                .build();
    }

    public static IngredientDTO mockHavsaltDTOToUpdate() {
        return IngredientDTO.builder()
                .id("01a50907-8141-4dd1-acdf-c4384669c2b2")
                .name("Havsalt")
                .description("Havsalt fra middelhavet i store flager")
                .createdBy("Jens")
//                .updatedBy("Jens")
//                .updated(OffsetDateTimeProvider.provide("2023-05-25T14:25:15"))
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-04-05T19:47:29"))
                .build();
    }


    public static IngredientDTO mockNewHavsaltDTO() {
        return IngredientDTO.builder()
                .name("Havsalt")
                .description("Havsalt fra middelhavet")
                .createdBy("Majken")
                .build();
    }
}
