package dk.serik.recipes.service;

import dk.serik.recipes.dto.UpdateUserRequestDTO;
import dk.serik.recipes.dto.UserDTO;

import java.util.List;

/** Accounts, as an admin manages them. Creating one is registration's, in {@link AuthService}. */
public interface UserService {

	List<UserDTO> findAll();

	/** Enables or disables another account, and makes it an admin or not. */
	void update(String id, UpdateUserRequestDTO request);
}
