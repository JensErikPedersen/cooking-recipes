package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.TagDTO;
import dk.serik.recipes.mockutil.MockTagUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TagMapperTest {
    @Test
    @DisplayName("Given Entity is OK, When Map to DTO, Then Ok")
    public void passMapperFromValidEntityToDto() {
        TagDTO mappedDto = TagMapper.from(MockTagUtil.mockTagSweet());
        Assertions.assertThat(mappedDto).isNotNull();
        Assertions.assertThat(mappedDto).isEqualTo(MockTagUtil.mockSweetTagDTO());
    }


    @Test
    @DisplayName("Given Entity is null, When mapped to DTO, Then DTO is Null")
    public void passMapperFromNullEntityToNullDto() {
        TagDTO mappedDto = TagMapper.from(null);
        Assertions.assertThat(mappedDto).isNull();
    }


}
