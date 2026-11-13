package dk.serik.recipes;

import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.service.PictureSize;
import dk.serik.recipes.service.PictureStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import static dk.serik.recipes.testutil.TestPictures.decode;
import static dk.serik.recipes.testutil.TestPictures.jpeg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The picture library through the whole stack: the multipart upload, the row, the three files on
 * disk ({@code app.pictures.dir}) and serving them. The 10 MB limit is Tomcat's, which MockMvc does
 * not run: e2e's {@code pictures-api.spec.ts} proves it, through the Next.js proxy as well.
 */
@SpringBootTest(classes = RecipesApplication.class)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@WithMockUser(username = "it-user")
class PictureIT {

	private static final String PICTURES = "/api/v1/pictures";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PictureStorage storage;

	@Test
	@DisplayName("Given a JPEG, a name and an alt text, When POST, Then 201 with Location, the row and three files")
	void shouldUpload() throws Exception {
		String response = mockMvc.perform(upload(jpeg(2000, 1500), "Rugbrød på bræt", "Et skåret rugbrød på et bræt"))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.startsWith(PICTURES + "/")))
				.andExpect(jsonPath("$.name").value("Rugbrød på bræt"))
				.andExpect(jsonPath("$.altText").value("Et skåret rugbrød på et bræt"))
				.andExpect(jsonPath("$.createdBy").value("it-user"))
				.andReturn().getResponse().getContentAsString();
		String id = jsonMapper.readTree(response).get("id").asString();

		mockMvc.perform(get(PICTURES + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Rugbrød på bræt"))
				.andExpect(jsonPath("$.altText").value("Et skåret rugbrød på et bræt"));
		mockMvc.perform(get(PICTURES))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(id)));
		for (PictureSize size : PictureSize.values()) {
			assertThat(storage.file(UUID.fromString(id), size)).exists();
		}
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(PictureSize.class)
	@DisplayName("Given an uploaded picture, When GET a size, Then a JPEG of that width, kept by the browser for a year")
	void shouldServeEachSize(PictureSize size) throws Exception {
		String id = create(jpeg(2000, 1000));

		byte[] body = mockMvc.perform(get(PICTURES + "/" + id + "/" + size.key()))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=31536000, private, immutable"))
				.andExpect(header().doesNotExist(HttpHeaders.PRAGMA))
				.andExpect(header().doesNotExist(HttpHeaders.EXPIRES))
				.andReturn().getResponse().getContentAsByteArray();

		BufferedImage picture = decode(body);
		assertThat(picture.getWidth()).isEqualTo(size.width());
		assertThat(picture.getHeight()).isEqualTo(size.width() / 2);
	}

	@Test
	@DisplayName("Given a picture, When GET an unknown size, Then 404")
	void shouldRefuseAnUnknownSize() throws Exception {
		String id = create(jpeg(500, 400));

		mockMvc.perform(get(PICTURES + "/" + id + "/huge"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.PICTURE_NOT_FOUND.getCode()));
	}

	@Test
	@DisplayName("Given no such picture, When GET it or a size of it, Then 404")
	void shouldRefuseAnUnknownPicture() throws Exception {
		String id = UUID.randomUUID().toString();

		mockMvc.perform(get(PICTURES + "/" + id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.PICTURE_NOT_FOUND.getCode()));
		mockMvc.perform(get(PICTURES + "/" + id + "/small"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.PICTURE_NOT_FOUND.getCode()));
	}

	@Test
	@DisplayName("Given a file that is not a picture, When POST, Then 400 on the file field, and no row")
	void shouldRefuseWhatIsNotAPicture() throws Exception {
		int before = count();

		mockMvc.perform(upload("plain text".getBytes(StandardCharsets.UTF_8), "Tekst", "Ingen billede"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.PICTURE_UNREADABLE.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("file"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("The file is not a JPEG or PNG picture"));

		assertThat(count()).isEqualTo(before);
	}

	@Test
	@DisplayName("Given no file, no name and no alt text, When POST, Then one 400 naming all three fields")
	void shouldRequireFileNameAndAltText() throws Exception {
		mockMvc.perform(multipart(PICTURES).with(csrf()).param("name", " ").param("altText", ""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()))
				.andExpect(jsonPath("$.validationExceptions[*].objectName",
						org.hamcrest.Matchers.containsInAnyOrder("file", "name", "altText")))
				.andExpect(jsonPath("$.validationExceptions[*].message", hasItem("Alt text is required")));
	}

	@Test
	@DisplayName("Given a name of 256 characters, When POST, Then 400 on name")
	void shouldLimitTheName() throws Exception {
		mockMvc.perform(upload(jpeg(400, 300), "x".repeat(256), "Alt"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("name"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("Picture name must be at most 255 characters"));
	}

	@Test
	@DisplayName("Given a picture, When DELETE it, Then 204, the row and its files are gone, and a second DELETE is 404")
	void shouldDelete() throws Exception {
		String id = create(jpeg(800, 600));

		mockMvc.perform(delete(PICTURES + "/" + id).with(csrf())).andExpect(status().isNoContent());

		mockMvc.perform(get(PICTURES + "/" + id)).andExpect(status().isNotFound());
		for (PictureSize size : PictureSize.values()) {
			assertThat(Files.exists(storage.file(UUID.fromString(id), size))).isFalse();
		}
		mockMvc.perform(delete(PICTURES + "/" + id).with(csrf())).andExpect(status().isNotFound());
	}

	@Test
	@WithAnonymousUser
	@DisplayName("Given no sign-in, When GET a picture's file, Then 401 and no picture")
	void shouldRefuseSignedOut() throws Exception {
		mockMvc.perform(get(PICTURES + "/" + UUID.randomUUID() + "/small"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.AUTHENTICATION_REQUIRED.getCode()))
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, not(containsString("image"))));
	}

	private String create(byte[] picture) throws Exception {
		String response = mockMvc.perform(upload(picture, "Billede " + UUID.randomUUID().toString().substring(0, 8), "Alt"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return jsonMapper.readTree(response).get("id").asString();
	}

	private MockMultipartHttpServletRequestBuilder upload(byte[] file, String name, String altText) {
		return (MockMultipartHttpServletRequestBuilder) multipart(PICTURES)
				.file(new MockMultipartFile("file", "picture.jpg", MediaType.IMAGE_JPEG_VALUE, file))
				.param("name", name)
				.param("altText", altText)
				.with(csrf());
	}

	private int count() {
		return jdbcTemplate.queryForObject("select count(*) from picture", Integer.class);
	}
}
