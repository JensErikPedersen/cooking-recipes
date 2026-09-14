package dk.serik.recipes;

import tools.jackson.databind.json.JsonMapper;
import dk.serik.recipes.dto.CategoryDTO;
import dk.serik.recipes.dto.IngredientDTO;
import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.dto.RecipeIngredientDTO;
import dk.serik.recipes.dto.TagDTO;
import dk.serik.recipes.dto.UnitDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Exercises the JSON contract against the <em>real</em> application context.
 * <p>
 * {@code @WebMvcTest} builds its own Jackson mapper, so controller slice tests can pass while
 * request bodies fail in production against a differently-configured one. That happened twice over:
 * a hand-rolled {@code @Primary ObjectMapper} lacked {@code ParameterNamesModule}, and
 * {@code @Jacksonized} sat on the class rather than the constructor and generated nothing. Every
 * write endpoint would have returned 500 with all slice tests green.
 * <p>
 * The same seam reopened on the move to Jackson 3: Lombok emits the Jackson 2 annotation pair unless
 * {@code lombok.jacksonized.jacksonVersion} says otherwise. These tests use the mapper the running
 * application actually uses, which is the only place that shows up.
 */
@SpringBootTest(classes = RecipesApplication.class)
@AutoConfigureTestDatabase
class JsonContractIT {

    @Autowired
    private JsonMapper objectMapper;

    /**
     * Spring Boot 4 does not expose the JSON converter as a bean - converters are assembled into
     * the MVC infrastructure instead. Reading it back off the handler adapter is also closer to
     * what this test means: the converter that actually handles request bodies.
     */
    @Autowired
    private RequestMappingHandlerAdapter handlerAdapter;

    @Test
    @DisplayName("Given the running application, When Spring MVC converts JSON, Then it uses the context JsonMapper")
    void mvcUsesTheContextObjectMapper() {
        JacksonJsonHttpMessageConverter converter = handlerAdapter.getMessageConverters().stream()
                .filter(JacksonJsonHttpMessageConverter.class::isInstance)
                .map(JacksonJsonHttpMessageConverter.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Spring MVC registered no Jackson JSON converter"));

        assertThat(converter.getMapper()).isSameAs(objectMapper);
    }

    @Test
    @DisplayName("Given a write payload for each DTO, When deserialised by the real mapper, Then it parses")
    void everyWriteDtoIsDeserialisable() {
        assertThatCode(() -> {
            objectMapper.readValue("{\"name\":\"Dessert\",\"description\":\"Sod\"}", CategoryDTO.class);
            objectMapper.readValue("{\"name\":\"Havsalt\",\"description\":\"Fra havet\"}", IngredientDTO.class);
            objectMapper.readValue("{\"label\":\"g\",\"name\":\"Gram\"}", UnitDTO.class);
            objectMapper.readValue("{\"name\":\"Mexi\"}", TagDTO.class);
            objectMapper.readValue("{\"ingredientId\":\"5f01d434-5a68-4359-9f2e-0a6793dce48d\","
                    + "\"unitId\":\"f7823293-7874-4459-9fb7-6b420a0627fa\",\"amount\":500}", RecipeIngredientDTO.class);
            objectMapper.readValue("{\"name\":\"Hvedebrod\",\"categoryId\":\"913a5159-3717-4b9d-a290-0158d31ea8ab\"}",
                    RecipeDTO.class);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Given a serialised RecipeDTO, When read back, Then the nested category survives the round trip")
    void recipeDtoRoundTripsSymmetrically() throws Exception {
        // the read shape must be accepted as a write shape, or GET -> edit -> POST cannot work
        RecipeDTO original = RecipeDTO.builder()
                .name("Hvedebrod")
                .description("Lakkert brod")
                .categoryId("913a5159-3717-4b9d-a290-0158d31ea8ab")
                .categoryName("Brod")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertThat(json).contains("\"category\"");

        RecipeDTO roundTripped = objectMapper.readValue(json, RecipeDTO.class);

        assertThat(roundTripped.getName()).isEqualTo("Hvedebrod");
        assertThat(roundTripped.getCategory()).isNotNull();
        assertThat(roundTripped.getCategory().getId()).isEqualTo("913a5159-3717-4b9d-a290-0158d31ea8ab");
        assertThat(roundTripped.getCategory().getName()).isEqualTo("Brod");
    }

    @Test
    @DisplayName("Given an OffsetDateTime field, When serialised, Then it is a formatted string, not a numeric timestamp")
    void datesSerialiseAsStrings() throws Exception {
        CategoryDTO dto = CategoryDTO.builder()
                .name("Dessert")
                .created(dk.serik.recipes.testutil.OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"))
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"created\":\"2023-01-25 14:25\"");
    }
}
