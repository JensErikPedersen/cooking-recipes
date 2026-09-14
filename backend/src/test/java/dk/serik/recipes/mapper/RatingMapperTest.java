package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.RatingDTO;
import dk.serik.recipes.mockutil.MockRatingUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RatingMapperTest {
    @Test
    @DisplayName("Given valid entity, When mapped by mapper, Then DTO is Ok")
    public void passMapperFromValidEntityToDto() {
        RatingDTO mappedDto = RatingMapper.from(MockRatingUtil.mockRating5());
        Assertions.assertThat(mappedDto).isNotNull();
        Assertions.assertThat(mappedDto).isEqualTo(MockRatingUtil.mockRatingDTO5());
    }

    @Test
    @DisplayName("Given Unit is null, When mapped to DTO, Then DTO is Null")
    public void passMapperFromNullEntityToNullDto() {
        RatingDTO mappedDto = RatingMapper.from(null);
        Assertions.assertThat(mappedDto).isNull();
    }

}
