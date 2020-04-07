package com.theagilemonkeys.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.theagilemonkeys.domain.UserEntity;
import com.theagilemonkeys.repository.UserRepository;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class DomainUserDetailsService implements UserDetailsService {

	private final Logger log = LoggerFactory.getLogger(DomainUserDetailsService.class);

	private final UserRepository userRepository;

	public DomainUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional
	public UserDetails loadUserByUsername(final String login) {
		log.debug("Authenticating {}", login);
		String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
		Optional<UserEntity> userFromDatabase = userRepository.findOneWithAuthoritiesByLogin(lowercaseLogin);
		if (!userFromDatabase.isPresent()) {
			return new User(lowercaseLogin, StringUtils.EMPTY, new ArrayList<>());
		}

		return userFromDatabase.map(user -> {
			List<GrantedAuthority> authorities = user.getAuthorities().stream()
					.map((authority) -> new SimpleGrantedAuthority(authority.getName())).collect(Collectors.toList());
			return new User(lowercaseLogin, StringUtils.EMPTY, authorities);
		}).orElseThrow(
				() -> new UsernameNotFoundException("User " + lowercaseLogin + " could not be found in the database"));
	}
}