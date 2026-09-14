package dk.serik.recipes.mockutil;

import dk.serik.recipes.dto.TagDTO;
import dk.serik.recipes.model.Tag;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MockTagUtil {

    public static Tag mockTagSweet() {
        Tag mock = new Tag();
        mock.setName("Sødt");
        mock.setCreatedBy("Majken");
        mock.setUpdatedBy("Jens");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("1dbdaf66-0e50-4a7f-868e-2689da2bc32a"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }

    public static Tag mockTagMexi() {
        Tag mock = new Tag();
        mock.setName("Mexi");
        mock.setCreatedBy("Jens");
        mock.setUpdatedBy("Majken");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("0f569775-68aa-44c1-94b5-1c694dec8890"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-05-13T11:43:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2023-03-21T10:12:29"));
        return mock;
    }

    public static Tag mockTagMexiUpdated() {
        Tag mock = new Tag();
        mock.setName("Meximad");
        mock.setCreatedBy("Jens");
        mock.setUpdatedBy("Majken");
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("0f569775-68aa-44c1-94b5-1c694dec8890"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-05-13T11:43:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2023-03-21T10:12:29"));
        return mock;
    }

    public static List<Tag> mockTagList() {
        return Arrays.asList(new Tag[]{mockTagMexi(), mockTagSweet()});
    }

    public static TagDTO mockSweetTagDTO() {
        TagDTO mock = TagDTO.builder()
                .id("1dbdaf66-0e50-4a7f-868e-2689da2bc32a")
                .createdBy("Majken")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"))
                .updatedBy("Jens")
                .updated(OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"))
                .name("Sødt")
                .build();
        return mock;
    }

    public static TagDTO mockMexiTagDTO() {
        TagDTO mock = TagDTO.builder()
            .id("0f569775-68aa-44c1-94b5-1c694dec8890")
            .createdBy("Jens")
            .created(OffsetDateTimeProvider.provideIsoFullFormat("2023-03-21T10:12:29"))
            .updatedBy("Majken")
            .updated(OffsetDateTimeProvider.provideIsoFullFormat("2023-05-13T11:43:15"))
            .name("Mexi")
            .build();
        return mock;
    }

    public static TagDTO mockMexiTagDTOToBeUpdated() {
        TagDTO mock = TagDTO.builder()
                .id("0f569775-68aa-44c1-94b5-1c694dec8890")
                .createdBy("Jens")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2023-03-21T10:12:29"))
                .updatedBy("Majken")
                .updated(OffsetDateTimeProvider.provideIsoFullFormat("2023-05-13T11:43:15"))
                .name("Meximad")
                .build();
        return mock;
    }

    public static List<TagDTO> mockTagDtos() {
        return Arrays.asList(new TagDTO[]{mockMexiTagDTO(), mockSweetTagDTO()});
    }
}
