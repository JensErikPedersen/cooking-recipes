package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.CategoryDTO;
import dk.serik.recipes.model.Category;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

public class CategoryMapperTest {
    @Test
    @DisplayName("Given valid entity, When mapped by mapper, Then DTO is Ok")
    public void passMapperFromEntityValidEntityToDto() {
        CategoryDTO mappedDto = CategoryMapper.from(mockBread());
        Assertions.assertThat(mappedDto).isNotNull();
        Assertions.assertThat(mappedDto).isEqualTo(mockCategoryDTO());
    }

    @Test
    @DisplayName("Given Unit is null, When mapped to DTO, Then DTO is Null")
    public void passMapperFromEntityNullEntityToNullDto() {
        CategoryDTO mappedDto = CategoryMapper.from(null);
        Assertions.assertThat(mappedDto).isNull();
    }

    private Category mockBread() {
        Category mock = new Category();
        mock.setName("Brød");
        mock.setDescription("Brød til alle måltider");
        mock.setUpdatedBy("Jens");
        mock.setCreatedBy("Majken");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("bfee03d7-3b55-418c-a867-1b3a9e85a22b"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }

    private CategoryDTO mockCategoryDTO() {
        CategoryDTO dto = CategoryDTO.builder()
                .id("bfee03d7-3b55-418c-a867-1b3a9e85a22b")
                .name("Brød")
                .description("Brød til alle måltider")
                .createdBy("Majken")
                .updatedBy("Jens")
                .updated(OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"))
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"))
                .build();
        return dto;
    }
}
