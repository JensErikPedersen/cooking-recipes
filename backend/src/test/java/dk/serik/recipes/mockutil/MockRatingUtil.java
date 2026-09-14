package dk.serik.recipes.mockutil;

import dk.serik.recipes.dto.RatingDTO;
import dk.serik.recipes.model.Rating;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

public class MockRatingUtil {

    public static Rating mockRating5() {
        Rating mock = new Rating();
        mock.setRating(5);
        mock.setUpdatedBy("Jens");
        mock.setCreatedBy("Majken");
        mock.setDescription("Outstanding");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("7c89ec02-63b9-4d68-9720-c22396fca1c7"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }

    public static Rating mockRating4() {
        Rating mock = new Rating();
        mock.setRating(4);
        mock.setUpdatedBy("Majken");
        mock.setCreatedBy("Jens");
        mock.setDescription("Very good");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("66d5730a-4221-4fe6-bb88-0120ddf96c5c"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }

    public static RatingDTO mockRatingDTO5() {
        RatingDTO dto = RatingDTO.builder()
                .id("7c89ec02-63b9-4d68-9720-c22396fca1c7")
                .rating(5)
                .description("Outstanding")
                .createdBy("Majken")
                .updatedBy("Jens")
                .updated(OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"))
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"))
                .build();
        return dto;
    }

    public static RatingDTO mockRatingDTO5ToBeUpdated() {
        RatingDTO dto = RatingDTO.builder()
                .id("7c89ec02-63b9-4d68-9720-c22396fca1c7")
                .rating(5)
                .description("Fantastic")
                .createdBy("Majken")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"))
                .build();
        return dto;
    }

    public static List<Rating> mockAllRatings() {
        return List.of(mockRating4(), mockRating5());
    }

    public static Rating mockRating5Updated() {
        Rating mock = new Rating();
        mock.setRating(5);
        mock.setUpdatedBy("Majken");
        mock.setCreatedBy("Jens");
        mock.setDescription("Fantastic");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("7c89ec02-63b9-4d68-9720-c22396fca1c7"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }
}
