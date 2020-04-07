package com.theagilemonkeys.web.rest;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.theagilemonkeys.config.Constants;
import com.theagilemonkeys.security.AuthoritiesConstants;
import com.theagilemonkeys.service.UserService;
import com.theagilemonkeys.service.dto.UserDTO;
import com.theagilemonkeys.web.rest.vm.ManagedUserVM;

import io.github.jhipster.web.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api")
public class UserResource {

	private final Logger log = LoggerFactory.getLogger(UserResource.class);
	private static final String USERS_ENDPOINT = "/users";

	@Value("${jhipster.clientApp.name}")
	private String applicationName;

	private final UserService userService;

	public UserResource(UserService userService) {
		this.userService = userService;
	}

	@GetMapping(USERS_ENDPOINT)
	public ResponseEntity<List<UserDTO>> getAllUsers(Pageable pageable) {
		final Page<UserDTO> page = userService.getAllManagedUsers(pageable);
		HttpHeaders headers = PaginationUtil
				.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
		return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
	}

	@GetMapping(USERS_ENDPOINT + "/authorities")
	@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
	public List<String> getAuthorities() {
		return userService.getAuthorities();
	}

	@GetMapping(USERS_ENDPOINT + "/{login:" + Constants.LOGIN_REGEX + "}")
	public ResponseEntity<UserDTO> getUser(@PathVariable String login) {
		log.debug("REST request to get User : {}", login);
		return ResponseUtil.wrapOrNotFound(userService.getUserWithAuthoritiesByLogin(login).map(UserDTO::new));
	}

	@PostMapping(USERS_ENDPOINT)
	@PreAuthorize("@securityChecker.canCreateUser(authentication)")
	public ResponseEntity<UserDTO> create(@Valid @RequestBody ManagedUserVM managedUserVM) throws URISyntaxException {
		log.debug("REST request to create User : {}", managedUserVM.getLogin());
		UserDTO newUser = userService.createUser(managedUserVM);
		return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
	}

	@PutMapping(USERS_ENDPOINT)
	@PreAuthorize("@securityChecker.canUpdateUser(authentication)")
	public ResponseEntity<UserDTO> update(@Valid @RequestBody ManagedUserVM managedUserVM) {
		log.debug("REST request to create User : {}", managedUserVM.getLogin());
		Optional<UserDTO> updatedUser = userService.updateUser(managedUserVM);

		if (updatedUser.isPresent()) {
			return ResponseEntity.status(HttpStatus.CREATED).body(updatedUser.get());
		} else {
			return ResponseEntity.badRequest().body(null);
		}
	}

	@DeleteMapping(USERS_ENDPOINT + "/{login:" + Constants.LOGIN_REGEX + "}")
	@PreAuthorize("@securityChecker.canDeleteUser(authentication)")
	public ResponseEntity<Void> delete(@PathVariable String login) {
		userService.deleteUser(login);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
