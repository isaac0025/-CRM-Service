package com.theagilemonkeys.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component("securityChecker")
public class SecurityChecker {

	private final UserDetailsService userDetailsService;

	public SecurityChecker(UserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}

	public boolean canCreateUser(Authentication authentication) {
		return this.isAdmin();
	}

	public boolean canUpdateUser(Authentication authentication) {
		return this.isAdmin();
	}

	public boolean canDeleteUser(Authentication authentication) {
		return this.isAdmin();
	}

	private boolean isAdmin() {
		return hasAuthority(AuthoritiesConstants.ADMIN);

	}

	private boolean isUser() {
		return hasAuthority(AuthoritiesConstants.ADMIN);

	}

	private boolean hasAuthority(String authorityName) {
		Optional<String> login = SecurityUtils.getCurrentUserLogin();

		if (!login.isPresent()) {
			return false;
		}

		return userDetailsService.loadUserByUsername(login.get()).getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals(authorityName));
	}
}
