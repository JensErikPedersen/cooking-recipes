package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.IngredientDTO;
import dk.serik.recipes.mockutil.MockIngredientUtil;
import dk.serik.recipes.model.Ingredient;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

public class IngredientMapperTest {
    @Test
    @DisplayName("Given Category is OK, When Map to DTO, Then Ok")
    public void passMapperFromValidEntityToDto() {
        IngredientDTO mappedDto = IngredientMapper.from(MockIngredientUtil.mockHvedemel());
        Assertions.assertThat(mappedDto).isNotNull();
        Assertions.assertThat(mappedDto).isEqualTo(MockIngredientUtil.mockHvedemelDTO());
    }


    @Test
    @DisplayName("Given Unit is null, When mapped to DTO, Then DTO is Null")
    public void passMapperFromNullEntityToNullDto() {
       IngredientDTO mappedDto = IngredientMapper.from(null);
       Assertions.assertThat(mappedDto).isNull();
    }

    private Ingredient mockHvedemel() {
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

    private IngredientDTO mockIngredientHvedemelDTO() {
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

}
