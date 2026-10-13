package dk.serik.recipes.controllers;

import dk.serik.recipes.dto.UpdateUserRequestDTO;
import dk.serik.recipes.dto.UserDTO;
import dk.serik.recipes.service.UserService;
import dk.serik.recipes.validator.UUID;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Accounts, for admins only - {@code SecurityConfig} refuses anyone else. No create: accounts come
 * from registration. No delete: an account is disabled instead, so what it owns keeps its owner.
 * <p>
 * The PUT answers 204, not the account: the change is plain SQL, and the account read back in the
 * same transaction would be the one JPA loaded before it.
 */
@RestController
@RequestMapping(UserController.PATH)
@AllArgsConstructor
public class UserController {

	static final String PATH = "/api/v1/users";

	private final UserService userService;

	@GetMapping
	public List<UserDTO> findAll() {
		return userService.findAll();
	}

	@PutMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void update(@PathVariable @UUID String id, @Valid @RequestBody UpdateUserRequestDTO request) {
		userService.update(id, request);
	}
}
