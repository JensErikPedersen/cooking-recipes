package dk.serik.recipes.mockutil;

import dk.serik.recipes.dto.CategoryDTO;
import dk.serik.recipes.model.Category;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

public class MockCategoryUtil {
    public static  Category dessertToBeSaved() {
        Category entity = new Category();
        entity.setName("Dessert");
        entity.setDescription("Den søde afrundning på en god middag");
        ReflectionTestUtils.setField(entity, "id", UUID.fromString("29af0d97-b11c-4ca4-82f5-e938a1234c25"));
        return entity;
    }

    public static  Category mockDessert() {
        Category entity = new Category();
        entity.setName("Dessert");
        entity.setDescription("Den søde afrundning på en god middag");
        ReflectionTestUtils.setField(entity, "id", UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8aa"));
        entity.setCreatedBy("Jens");
        return entity;
    }

    public static  Category mockBread() {
        Category entity = new Category();
        entity.setName("Brød");
        entity.setDescription("Alle slags brød");
        ReflectionTestUtils.setField(entity, "id", UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8ab"));
        entity.setCreatedBy("Jens");
        return entity;
    }

    public static  Category mockUpdatedDessert() {
        Category entity = new Category();
        entity.setName("Dessert");
        entity.setDescription("Til alle med en sød tand");
        ReflectionTestUtils.setField(entity, "id", UUID.fromString("913a5159-3717-4b9d-a290-0158d31ea8aa"));
        entity.setCreatedBy("Jens");
        return entity;
    }

    public static  CategoryDTO mockToBeSavedDessertDTO() {
        CategoryDTO dto = CategoryDTO.builder()
                .name("Dessert")
                .description("Den søde afrundning på en god middag")
                .id("913a5159-3717-4b9d-a290-0158d31ea8aa").build();
        return dto;
    }

    public static CategoryDTO mockToBeUpdatedDessertDTO() {
        CategoryDTO dto = CategoryDTO.builder()
                .name("Dessert")
                .description("Til alle med en sød tand")
                .id("913a5159-3717-4b9d-a290-0158d31ea8aa").build();
        return dto;
    }

    public static Category updatedDessert() {
        Category entity = new Category();
        entity.setName("Dessert");
        entity.setDescription("Til alle med en sød tand");
        ReflectionTestUtils.setField(entity, "id", "913a5159-3717-4b9d-a290-0158d31ea8aa");
        return entity;
    }

}
