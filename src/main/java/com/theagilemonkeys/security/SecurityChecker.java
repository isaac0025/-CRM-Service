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

	public boolean canSearchUser(Authentication authentication) {
		return this.isAdmin();
	}

	public boolean canGetAuthorities(Authentication authentication) {
		return this.isAdmin();
	}

	public boolean canListUsers(Authentication authentication) {
		return this.isAdmin();
	}

	public boolean canUpdateUser(Authentication authentication) {
		return this.isAdmin();
	}

	public boolean canDeleteUser(Authentication authentication) {
		return this.isAdmin();
	}

	public boolean canSearchCustomer(Authentication authentication) {
		return this.isAdmin() || this.isUser();
	}

	public boolean canListCustomers(Authentication authentication) {
		return this.isAdmin() || this.isUser();
	}

	public boolean canCreateCustomer(Authentication authentication) {
		return this.isAdmin() || this.isUser();
	}

	public boolean canUpdateCustomer(Authentication authentication) {
		return this.isAdmin() || this.isUser();
	}

	public boolean canDeleteCustomer(Authentication authentication) {
		return this.isAdmin() || this.isUser();
	}

	private boolean isAdmin() {
		return hasAuthority(AuthoritiesConstants.ADMIN);

	}

	private boolean isUser() {
		return hasAuthority(AuthoritiesConstants.USER);

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
