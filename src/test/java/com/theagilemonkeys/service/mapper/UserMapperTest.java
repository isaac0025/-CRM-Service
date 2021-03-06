package com.theagilemonkeys.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.theagilemonkeys.domain.UserEntity;
import com.theagilemonkeys.service.dto.UserDTO;

/**
 * Unit tests for {@link UserMapper}.
 */
public class UserMapperTest {

	private static final String DEFAULT_LOGIN = "johndoe";
	private static final Long DEFAULT_ID = 1L;

	private UserMapper userMapper;
	private UserEntity user;
	private UserDTO userDto;

	@BeforeEach
	public void init() {
		userMapper = new UserMapper();
		user = new UserEntity();
		user.setLogin(DEFAULT_LOGIN);
		user.setEmail("johndoe@localhost");
		user.setFirstName("john");
		user.setLastName("doe");
		user.setLangKey("en");

		userDto = new UserDTO(user);
	}

	@Test
	public void usersToUserDTOsShouldMapOnlyNonNullUsers() {
		List<UserEntity> users = new ArrayList<>();
		users.add(user);
		users.add(null);

		List<UserDTO> userDTOS = userMapper.usersToUserDTOs(users);

		assertThat(userDTOS).isNotEmpty();
		assertThat(userDTOS).size().isEqualTo(1);
	}

	@Test
	public void userDTOsToUsersShouldMapOnlyNonNullUsers() {
		List<UserDTO> usersDto = new ArrayList<>();
		usersDto.add(userDto);
		usersDto.add(null);

		List<UserEntity> users = userMapper.userDTOsToUsers(usersDto);

		assertThat(users).isNotEmpty();
		assertThat(users).size().isEqualTo(1);
	}

	@Test
	public void userDTOsToUsersWithAuthoritiesStringShouldMapToUsersWithAuthoritiesDomain() {
		Set<String> authoritiesAsString = new HashSet<>();
		authoritiesAsString.add("ADMIN");
		userDto.setAuthorities(authoritiesAsString);

		List<UserDTO> usersDto = new ArrayList<>();
		usersDto.add(userDto);

		List<UserEntity> users = userMapper.userDTOsToUsers(usersDto);

		assertThat(users).isNotEmpty();
		assertThat(users).size().isEqualTo(1);
		assertThat(users.get(0).getAuthorities()).isNotNull();
		assertThat(users.get(0).getAuthorities()).isNotEmpty();
		assertThat(users.get(0).getAuthorities().iterator().next().getName()).isEqualTo("ADMIN");
	}

	@Test
	public void userDTOsToUsersMapWithNullAuthoritiesStringShouldReturnUserWithEmptyAuthorities() {
		userDto.setAuthorities(null);

		List<UserDTO> usersDto = new ArrayList<>();
		usersDto.add(userDto);

		List<UserEntity> users = userMapper.userDTOsToUsers(usersDto);

		assertThat(users).isNotEmpty();
		assertThat(users).size().isEqualTo(1);
		assertThat(users.get(0).getAuthorities()).isNotNull();
		assertThat(users.get(0).getAuthorities()).isEmpty();
	}

	@Test
	public void userDTOToUserMapWithAuthoritiesStringShouldReturnUserWithAuthorities() {
		Set<String> authoritiesAsString = new HashSet<>();
		authoritiesAsString.add("ADMIN");
		userDto.setAuthorities(authoritiesAsString);

		UserEntity user = userMapper.userDTOToUser(userDto);

		assertThat(user).isNotNull();
		assertThat(user.getAuthorities()).isNotNull();
		assertThat(user.getAuthorities()).isNotEmpty();
		assertThat(user.getAuthorities().iterator().next().getName()).isEqualTo("ADMIN");
	}

	@Test
	public void userDTOToUserMapWithNullAuthoritiesStringShouldReturnUserWithEmptyAuthorities() {
		userDto.setAuthorities(null);

		UserEntity user = userMapper.userDTOToUser(userDto);

		assertThat(user).isNotNull();
		assertThat(user.getAuthorities()).isNotNull();
		assertThat(user.getAuthorities()).isEmpty();
	}

	@Test
	public void userDTOToUserMapWithNullUserShouldReturnNull() {
		assertThat(userMapper.userDTOToUser(null)).isNull();
	}

	@Test
	public void testUserFromId() {
		assertThat(userMapper.userFromId(DEFAULT_ID).getId()).isEqualTo(DEFAULT_ID);
		assertThat(userMapper.userFromId(null)).isNull();
	}
}
