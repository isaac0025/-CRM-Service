package com.theagilemonkeys.service.mapper;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.theagilemonkeys.domain.Authority;
import com.theagilemonkeys.domain.UserEntity;
import com.theagilemonkeys.service.dto.UserDTO;

/**
 * Mapper for the entity {@link UserEntity} and its DTO called {@link UserDTO}.
 *
 * Normal mappers are generated using MapStruct, this one is hand-coded as
 * MapStruct support is still in beta, and requires a manual step with an IDE.
 */
@Service
public class UserMapper {

	public List<UserDTO> usersToUserDTOs(List<UserEntity> users) {
		return users.stream().filter(Objects::nonNull).map(this::userToUserDTO).collect(Collectors.toList());
	}

	public UserDTO userToUserDTO(UserEntity user) {
		return new UserDTO(user);
	}

	public List<UserEntity> userDTOsToUsers(List<UserDTO> userDTOs) {
		return userDTOs.stream().filter(Objects::nonNull).map(this::userDTOToUser).collect(Collectors.toList());
	}

	public UserEntity userDTOToUser(UserDTO userDTO) {
		if (userDTO == null) {
			return null;
		} else {
			UserEntity user = new UserEntity();
			user.setId(userDTO.getId());
			user.setLogin(userDTO.getLogin());
			user.setFirstName(userDTO.getFirstName());
			user.setLastName(userDTO.getLastName());
			user.setEmail(userDTO.getEmail());
			user.setLangKey(userDTO.getLangKey());
			Set<Authority> authorities = this.authoritiesFromStrings(userDTO.getAuthorities());
			user.setAuthorities(authorities);
			return user;
		}
	}

	public UserEntity updateFromDTO(UserEntity user, UserDTO userDTO) {
		user.setFirstName(userDTO.getFirstName());
		user.setLastName(userDTO.getLastName());
		user.setEmail(userDTO.getEmail());
		user.setLangKey(userDTO.getLangKey());
		Set<Authority> authorities = this.authoritiesFromStrings(userDTO.getAuthorities());
		user.setAuthorities(authorities);
		return user;
	}

	private Set<Authority> authoritiesFromStrings(Set<String> authoritiesAsString) {
		Set<Authority> authorities = new HashSet<>();

		if (authoritiesAsString != null) {
			authorities = authoritiesAsString.stream().map(string -> {
				Authority auth = new Authority();
				auth.setName(string);
				return auth;
			}).collect(Collectors.toSet());
		}

		return authorities;
	}

	public UserEntity userFromId(Long id) {
		if (id == null) {
			return null;
		}
		UserEntity user = new UserEntity();
		user.setId(id);
		return user;
	}
}
