package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.UserDTO;
import dk.serik.recipes.model.AppUser;

import java.util.Objects;

public class UserMapper {
	public static UserDTO from(AppUser entity) {
		if (Objects.isNull(entity)) {
			return null;
		}
		return UserDTO.builder()
				.id(entity.getId().toString())
				.username(entity.getUsername())
				.enabled(entity.isEnabled())
				.roles(entity.roleList())
				.createdBy(entity.getCreatedBy())
				.created(entity.getCreated())
				.updatedBy(entity.getUpdatedBy())
				.updated(entity.getUpdated())
				.build();
	}
}
