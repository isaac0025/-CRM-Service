package com.theagilemonkeys.service;

import com.theagilemonkeys.config.Constants;
import com.theagilemonkeys.domain.Authority;
import com.theagilemonkeys.domain.UserEntity;
import com.theagilemonkeys.repository.AuthorityRepository;
import com.theagilemonkeys.repository.UserRepository;
import com.theagilemonkeys.security.SecurityUtils;
import com.theagilemonkeys.service.dto.UserDTO;
import com.theagilemonkeys.service.mapper.UserMapper;

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

import java.util.*;
import java.util.stream.Collectors;

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

	public void updateUser(String firstName, String lastName, String email, String langKey, String imageUrl) {
		SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin).ifPresent(user -> {
			user.setFirstName(firstName);
			user.setLastName(lastName);
			if (email != null) {
				user.setEmail(email.toLowerCase());
			}
			user.setLangKey(langKey);
			user.setImageUrl(imageUrl);
			this.clearUserCaches(user);
			log.debug("Changed Information for User: {}", user);
		});
	}

	public Optional<UserDTO> updateUser(UserDTO userDTO) {
		return Optional.of(userRepository.findById(userDTO.getId())).filter(Optional::isPresent).map(Optional::get)
				.map(user -> {
					this.clearUserCaches(user);
					user.setLogin(userDTO.getLogin().toLowerCase());
					user.setFirstName(userDTO.getFirstName());
					user.setLastName(userDTO.getLastName());
					if (userDTO.getEmail() != null) {
						user.setEmail(userDTO.getEmail().toLowerCase());
					}
					user.setImageUrl(userDTO.getImageUrl());
					user.setActivated(userDTO.isActivated());
					user.setLangKey(userDTO.getLangKey());
					Set<Authority> managedAuthorities = user.getAuthorities();
					managedAuthorities.clear();
					userDTO.getAuthorities().stream().map(authorityRepository::findById).filter(Optional::isPresent)
							.map(Optional::get).forEach(managedAuthorities::add);
					this.clearUserCaches(user);
					log.debug("Changed Information for User: {}", user);
					return user;
				}).map(UserDTO::new);
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

	private static UserEntity getUser(Map<String, Object> details) {
		UserEntity user = new UserEntity();
		// handle resource server JWT, where sub claim is email and uid is ID
		if (details.get("uid") != null) {
			user.setId((String) details.get("uid"));
			user.setLogin((String) details.get("sub"));
		} else {
			user.setId((String) details.get("sub"));
		}
		if (details.get("preferred_username") != null) {
			user.setLogin(((String) details.get("preferred_username")).toLowerCase());
		} else if (user.getLogin() == null) {
			user.setLogin(user.getId());
		}
		if (details.get("given_name") != null) {
			user.setFirstName((String) details.get("given_name"));
		}
		if (details.get("family_name") != null) {
			user.setLastName((String) details.get("family_name"));
		}
		if (details.get("email_verified") != null) {
			user.setActivated((Boolean) details.get("email_verified"));
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
		if (details.get("picture") != null) {
			user.setImageUrl((String) details.get("picture"));
		}
		user.setActivated(false);
		return user;
	}

	private void clearUserCaches(UserEntity user) {
		Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evict(user.getLogin());
		if (user.getEmail() != null) {
			Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).evict(user.getEmail());
		}
	}
}
