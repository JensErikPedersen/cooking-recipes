package dk.serik.recipes.controllers;

import dk.serik.recipes.dto.UpdateUserRequestDTO;
import dk.serik.recipes.dto.UserDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
// The security chain - who may call these at all - is UsersIT's business; these are about the controller.
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

	private static final String BASE = "/api/v1/users";
	private static final String ID = "0f569775-68aa-44c1-94b5-1c694dec8890";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	@DisplayName("Given accounts, When GET the collection, Then 200 and the list")
	void shouldReturnAllUsers() throws Exception {
		given(userService.findAll()).willReturn(List.of(UserDTO.builder()
				.id(ID).username("anna").enabled(true).roles(List.of("ROLE_USER")).build()));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].username").value("anna"))
				.andExpect(jsonPath("$[0].enabled").value(true))
				.andExpect(jsonPath("$[0].roles[0]").value("ROLE_USER"));
	}

	@Test
	@DisplayName("Given a valid body, When PUT, Then 204 and the service gets the path id and both values")
	void shouldUpdateUser() throws Exception {
		mockMvc.perform(put(BASE + "/" + ID).contentType(MediaType.APPLICATION_JSON)
						.content("{\"enabled\": false, \"admin\": true}"))
				.andExpect(status().isNoContent());

		ArgumentCaptor<UpdateUserRequestDTO> captor = ArgumentCaptor.forClass(UpdateUserRequestDTO.class);
		verify(userService).update(eq(ID), captor.capture());
		assertThat(captor.getValue().getEnabled()).isFalse();
		assertThat(captor.getValue().getAdmin()).isTrue();
	}

	@Test
	@DisplayName("Given a body without enabled, When PUT, Then 400 on the field enabled and no service call")
	void shouldRequireEnabled() throws Exception {
		mockMvc.perform(put(BASE + "/" + ID).contentType(MediaType.APPLICATION_JSON).content("{\"admin\": true}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("enabled"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("Enabled is required"));

		verify(userService, never()).update(any(), any());
	}

	@Test
	@DisplayName("Given a malformed id, When PUT, Then 400 and no service call")
	void shouldRejectMalformedId() throws Exception {
		mockMvc.perform(put(BASE + "/not-a-uuid").contentType(MediaType.APPLICATION_JSON)
						.content("{\"enabled\": true, \"admin\": false}"))
				.andExpect(status().isBadRequest());

		verify(userService, never()).update(any(), any());
	}

	@Test
	@DisplayName("Given the service refuses the admin's own account, When PUT, Then its 400 in the envelope")
	void shouldPassOnOwnAccountRefusal() throws Exception {
		willThrow(ServiceException.badRequest(ApplicationErrorCodes.OWN_ACCOUNT, "You cannot change your own account"))
				.given(userService).update(eq(ID), any());

		mockMvc.perform(put(BASE + "/" + ID).contentType(MediaType.APPLICATION_JSON)
						.content("{\"enabled\": false, \"admin\": false}"))
				.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.OWN_ACCOUNT.getCode()))
				.andExpect(jsonPath("$.message").value("You cannot change your own account"));
	}
}
