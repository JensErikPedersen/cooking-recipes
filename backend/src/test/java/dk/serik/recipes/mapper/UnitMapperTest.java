package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.UnitDTO;
import dk.serik.recipes.mockutil.MockUnitUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UnitMapperTest {
    @Test
    @DisplayName("Given Unit is OK, When Map to DTO, Then Ok")
    public void passMapperFromValidEntityToDto() {
        UnitDTO mappedDto = UnitMapper.from(MockUnitUtil.mockDl());
        Assertions.assertThat(mappedDto).isNotNull();
        Assertions.assertThat(mappedDto).isEqualTo(MockUnitUtil.mockUnitDeciliterDTO());
    }

    @Test
    @DisplayName("Given Unit is null, When mapped to DTO, Then DTO is Null")
    public void passMapperFromNullEntityToNullDto() {
        UnitDTO mappedDto = UnitMapper.from(null);
        Assertions.assertThat(mappedDto).isNull();
    }


}
