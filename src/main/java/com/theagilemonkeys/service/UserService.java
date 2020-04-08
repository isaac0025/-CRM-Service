package com.theagilemonkeys.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.theagilemonkeys.config.Constants;
import com.theagilemonkeys.domain.Authority;
import com.theagilemonkeys.domain.UserEntity;
import com.theagilemonkeys.repository.AuthorityRepository;
import com.theagilemonkeys.repository.UserRepository;
import com.theagilemonkeys.security.SecurityUtils;
import com.theagilemonkeys.service.dto.UserDTO;
import com.theagilemonkeys.service.mapper.UserMapper;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	private final AuthorityRepository authorityRepository;

	private final CacheManager cacheManager;

	private final UserMapper userMapper;

	public UserService(UserRepository userRepository, AuthorityRepository authorityRepository,
			CacheManager cacheManager, UserMapper userMapper) {
		this.userRepository = userRepository;
		this.authorityRepository = authorityRepository;
		this.cacheManager = cacheManager;
		this.userMapper = userMapper;
	}

	public Optional<UserDTO> updateUser(UserDTO userDTO) {

		Optional<UserEntity> existingUser = userRepository.findById(userDTO.getId());

		if (!existingUser.isPresent()) {
			return Optional.empty();
		}

		UserEntity updatedUser = userMapper.updateFromDTO(existingUser.get(), userDTO);
		updatedUser = userRepository.saveAndFlush(updatedUser);
		clearUserCaches(updatedUser);
		return Optional.ofNullable(userMapper.userToUserDTO(updatedUser));

	}

	public void deleteUser(String login) {
		userRepository.findOneByLogin(login).ifPresent(user -> {
			userRepository.delete(user);
			this.clearUserCaches(user);
			log.debug("Deleted User: {}", user);
		});
	}

	@Transactional(readOnly = true)
	public Page<UserDTO> getAllManagedUsers(Pageable pageable) {
		return userRepository.findAllByLoginNot(pageable, Constants.ANONYMOUS_USER).map(UserDTO::new);
	}

	@Transactional(readOnly = true)
	public Optional<UserEntity> getUserWithAuthoritiesByLogin(String login) {
		return userRepository.findOneWithAuthoritiesByLogin(login);
	}

	@Transactional(readOnly = true)
	public Optional<UserEntity> getUserWithAuthorities(Long id) {
		return userRepository.findOneWithAuthoritiesById(id);
	}

	@Transactional(readOnly = true)
	public Optional<UserEntity> getUserWithAuthorities() {
		return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByLogin);
	}

	public List<String> getAuthorities() {
		return authorityRepository.findAll().stream().map(Authority::getName).collect(Collectors.toList());
	}

	public UserDTO getUserFromAuthentication(AbstractAuthenticationToken authToken) {
		Map<String, Object> attributes;
		if (authToken instanceof OAuth2AuthenticationToken) {
			attributes = ((OAuth2AuthenticationToken) authToken).getPrincipal().getAttributes();
		} else if (authToken instanceof JwtAuthenticationToken) {
			attributes = ((JwtAuthenticationToken) authToken).getTokenAttributes();
		} else {
			throw new IllegalArgumentException("AuthenticationToken is not OAuth2 or JWT!");
		}

		String login = (String) attributes.get("preferred_username");
		Optional<UserEntity> user = userRepository.findOneByLogin(login);

		if (user.isPresent()) {
			return userMapper.userToUserDTO(user.get());
		} else {
			return userMapper.userToUserDTO(getUser(attributes));
		}
	}

	public UserDTO createUser(UserDTO userDTO) {
		UserEntity user = userMapper.userDTOToUser(userDTO);
		user = userRepository.saveAndFlush(user);
		return userMapper.userToUserDTO(user);
	}

	private static UserEntity getUser(Map<String, Object> details) {
		UserEntity user = new UserEntity();
		// handle resource server JWT, where sub claim is email and uid is ID
		if (details.get("preferred_username") != null) {
			user.setLogin(((String) details.get("preferred_username")).toLowerCase());
		} else if (user.getLogin() == null) {
			user.setLogin((String) details.get("sub"));
		}
		if (details.get("given_name") != null) {
			user.setFirstName((String) details.get("given_name"));
		}
		if (details.get("family_name") != null) {
			user.setLastName((String) details.get("family_name"));
		}
		if (details.get("email") != null) {
			user.setEmail(((String) details.get("email")).toLowerCase());
		} else {
			user.setEmail((String) details.get("sub"));
		}
		if (details.get("langKey") != null) {
			user.setLangKey((String) details.get("langKey"));
		} else if (details.get("locale") != null) {
			// trim off country code if it exists
			String locale = (String) details.get("locale");
			if (locale.contains("_")) {
				locale = locale.substring(0, locale.indexOf('_'));
			} else if (locale.contains("-")) {
				locale = locale.substring(0, locale.indexOf('-'));
			}
			user.setLangKey(locale.toLowerCase());
		} else {
			// set langKey to default if not specified by IdP
			user.setLangKey(Constants.DEFAULT_LANGUAGE);
		}
		return user;
	}

	private void clearUserCaches(UserEntity user) {
		Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evict(user.getLogin());
		if (user.getEmail() != null) {
			Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).evict(user.getEmail());
		}
	}
}
