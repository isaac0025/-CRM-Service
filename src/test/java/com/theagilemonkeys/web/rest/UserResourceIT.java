package com.theagilemonkeys.web.rest;

import static com.theagilemonkeys.web.rest.TestUtil.ID_TOKEN;
import static com.theagilemonkeys.web.rest.TestUtil.authenticationToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.theagilemonkeys.CrmApp;
import com.theagilemonkeys.config.TestSecurityConfiguration;
import com.theagilemonkeys.domain.Authority;
import com.theagilemonkeys.domain.UserEntity;
import com.theagilemonkeys.repository.UserRepository;
import com.theagilemonkeys.security.AuthoritiesConstants;
import com.theagilemonkeys.service.dto.UserDTO;
import com.theagilemonkeys.service.mapper.UserMapper;

/**
 * Integration tests for the {@link UserResource} REST controller.
 */
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
@SpringBootTest(classes = { CrmApp.class, TestSecurityConfiguration.class })
public class UserResourceIT {

	private static final String DEFAULT_LOGIN = "johndoe";

	private static final String ADMIN_LOGIN = "admin";

	private static final String DEFAULT_EMAIL = "johndoe@localhost";

	private static final String DEFAULT_FIRSTNAME = "john";

	private static final String DEFAULT_LASTNAME = "doe";

	private static final String DEFAULT_LANGKEY = "en";

	private static final String ENDPOINT = "/api/users";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private EntityManager em;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private MockMvc restUserMockMvc;

	@Autowired
	private WebApplicationContext context;

	private UserEntity user;

	private OidcIdToken idToken;

	@BeforeEach
	public void setup() {
		cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).clear();
		cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE).clear();
	}

	@BeforeEach
	public void before() throws Exception {
		Map<String, Object> claims = new HashMap<>();
		claims.put("groups", Collections.singletonList("ROLE_ADMIN"));
		claims.put("sub", ADMIN_LOGIN);
		claims.put("preferred_username", ADMIN_LOGIN);
		this.idToken = new OidcIdToken(ID_TOKEN, Instant.now(), Instant.now().plusSeconds(60), claims);

		SecurityContextHolder.getContext().setAuthentication(authenticationToken(idToken));
		SecurityContextHolderAwareRequestFilter authInjector = new SecurityContextHolderAwareRequestFilter();
		authInjector.afterPropertiesSet();

		this.restUserMockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	/**
	 * Create a User.
	 *
	 * This is a static method, as tests for other entities might also need it, if
	 * they test an entity which has a required relationship to the User entity.
	 */
	public static UserEntity createEntity(EntityManager em) {
		UserEntity user = new UserEntity();
		user.setLogin(DEFAULT_LOGIN + RandomStringUtils.randomAlphabetic(5));
		user.setEmail(RandomStringUtils.randomAlphabetic(5) + DEFAULT_EMAIL);
		user.setFirstName(DEFAULT_FIRSTNAME);
		user.setLastName(DEFAULT_LASTNAME);
		user.setLangKey(DEFAULT_LANGKEY);
		return user;
	}

	@BeforeEach
	public void initTest() {
		user = createEntity(em);
		user.setLogin(DEFAULT_LOGIN);
		user.setEmail(DEFAULT_EMAIL);
	}

	@Test
	@Transactional
	public void getAllUsers() throws Exception {
		// Initialize the database
		userRepository.saveAndFlush(user);

		// Get all the users
		restUserMockMvc.perform(get("/api/users?sort=id,desc").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN)))
				.andExpect(jsonPath("$.[*].firstName").value(hasItem(DEFAULT_FIRSTNAME)))
				.andExpect(jsonPath("$.[*].lastName").value(hasItem(DEFAULT_LASTNAME)))
				.andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
				.andExpect(jsonPath("$.[*].langKey").value(hasItem(DEFAULT_LANGKEY)));
	}

	@Test
	@Transactional
	public void createUser() throws Exception {
		int databaseSizeBeforeCreate = userRepository.findAll().size();
		UserDTO source = userMapper.userToUserDTO(user);

		restUserMockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(TestUtil.convertObjectToJsonBytes(source))).andExpect(status().isCreated());

		List<UserEntity> userList = userRepository.findAll().stream()
				.sorted((user1, user2) -> user2.getId().compareTo(user1.getId())).collect(Collectors.toList());
		assertThat(userList).hasSize(databaseSizeBeforeCreate + 1);
		UserEntity testUser = userList.get(0);
		assertThat(testUser.getLogin()).isEqualTo(source.getLogin());
		assertThat(testUser.getFirstName()).isEqualTo(source.getFirstName());
		assertThat(testUser.getLastName()).isEqualTo(source.getLastName());
		assertThat(testUser.getEmail()).isEqualTo(source.getEmail());

	}

	@Test
	@Transactional
	public void updateUser() throws Exception {

		Optional<UserEntity> user = userRepository.findOneByLogin(ADMIN_LOGIN);
		assertThat(user.isPresent());

		if (user.isPresent()) {
			UserDTO updatedUserDTO = userMapper.userToUserDTO(user.get());
			updatedUserDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));
			updatedUserDTO.setLogin(DEFAULT_LOGIN);
			updatedUserDTO.setFirstName(DEFAULT_FIRSTNAME);
			updatedUserDTO.setLastName(DEFAULT_LASTNAME);
			updatedUserDTO.setEmail(DEFAULT_EMAIL);

			restUserMockMvc
					.perform(put(ENDPOINT).contentType(MediaType.APPLICATION_JSON_VALUE)
							.content(TestUtil.convertObjectToJsonBytes(updatedUserDTO)))
					.andExpect(status().isCreated());

			Optional<UserEntity> updatedUser = userRepository.findById(user.get().getId());
			assertThat(updatedUser.isPresent());
			if (updatedUser.isPresent()) {
				assertThat(updatedUser.get().getLogin()).isEqualTo(ADMIN_LOGIN);
				assertThat(updatedUser.get().getFirstName()).isEqualTo(updatedUserDTO.getFirstName());
				assertThat(updatedUser.get().getLastName()).isEqualTo(updatedUserDTO.getLastName());
				assertThat(updatedUser.get().getEmail()).isEqualTo(updatedUserDTO.getEmail());
				assertThat(updatedUser.get().getAuthorities().stream().map(authority -> authority.getName())
						.collect(Collectors.toSet()))
								.isEqualTo((updatedUserDTO.getAuthorities().stream().collect(Collectors.toSet())));
			}

		}

	}

	@Test
	@Transactional
	public void getUser() throws Exception {
		// Initialize the database
		userRepository.saveAndFlush(user);

		assertThat(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).get(user.getLogin())).isNull();

		// Get the user
		restUserMockMvc.perform(get("/api/users/{login}", user.getLogin())).andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.login").value(user.getLogin()))
				.andExpect(jsonPath("$.firstName").value(DEFAULT_FIRSTNAME))
				.andExpect(jsonPath("$.lastName").value(DEFAULT_LASTNAME))
				.andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
				.andExpect(jsonPath("$.langKey").value(DEFAULT_LANGKEY));

		assertThat(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).get(user.getLogin())).isNotNull();
	}

	@Test
	@Transactional
	public void getNonExistingUser() throws Exception {
		restUserMockMvc.perform(get("/api/users/unknown")).andExpect(status().isNotFound());
	}

	@Test
	@Transactional
	public void getAllAuthorities() throws Exception {
		restUserMockMvc
				.perform(get("/api/users/authorities").accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").value(hasItems(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN)));
	}

	@Test
	@Transactional
	public void testUserEquals() throws Exception {
		TestUtil.equalsVerifier(UserEntity.class);
		UserEntity user1 = new UserEntity();
		user1.setId(10L);
		UserEntity user2 = new UserEntity();
		user2.setId(user1.getId());
		assertThat(user1).isEqualTo(user2);
		user2.setId(12L);
		assertThat(user1).isNotEqualTo(user2);
		user1.setId(11L);
		assertThat(user1).isNotEqualTo(user2);
	}

	@Test
	public void testUserDTOtoUser() {
		UserDTO userDTO = new UserDTO();
		userDTO.setLogin(DEFAULT_LOGIN);
		userDTO.setFirstName(DEFAULT_FIRSTNAME);
		userDTO.setLastName(DEFAULT_LASTNAME);
		userDTO.setEmail(DEFAULT_EMAIL);
		userDTO.setLangKey(DEFAULT_LANGKEY);
		userDTO.setCreatedBy(DEFAULT_LOGIN);
		userDTO.setLastModifiedBy(DEFAULT_LOGIN);
		userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

		UserEntity user = userMapper.userDTOToUser(userDTO);
		assertThat(user.getLogin()).isEqualTo(DEFAULT_LOGIN);
		assertThat(user.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
		assertThat(user.getLastName()).isEqualTo(DEFAULT_LASTNAME);
		assertThat(user.getEmail()).isEqualTo(DEFAULT_EMAIL);
		assertThat(user.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
		assertThat(user.getCreatedBy()).isNull();
		assertThat(user.getCreatedDate()).isNotNull();
		assertThat(user.getLastModifiedBy()).isNull();
		assertThat(user.getLastModifiedDate()).isNotNull();
		assertThat(user.getAuthorities()).extracting("name").containsExactly(AuthoritiesConstants.USER);
	}

	@Test
	public void testUserToUserDTO() {
		user.setCreatedBy(DEFAULT_LOGIN);
		user.setCreatedDate(Instant.now());
		user.setLastModifiedBy(DEFAULT_LOGIN);
		user.setLastModifiedDate(Instant.now());
		Set<Authority> authorities = new HashSet<>();
		Authority authority = new Authority();
		authority.setName(AuthoritiesConstants.USER);
		authorities.add(authority);
		user.setAuthorities(authorities);

		UserDTO userDTO = userMapper.userToUserDTO(user);

		assertThat(userDTO.getLogin()).isEqualTo(DEFAULT_LOGIN);
		assertThat(userDTO.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
		assertThat(userDTO.getLastName()).isEqualTo(DEFAULT_LASTNAME);
		assertThat(userDTO.getEmail()).isEqualTo(DEFAULT_EMAIL);
		assertThat(userDTO.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
		assertThat(userDTO.getCreatedBy()).isEqualTo(DEFAULT_LOGIN);
		assertThat(userDTO.getCreatedDate()).isEqualTo(user.getCreatedDate());
		assertThat(userDTO.getLastModifiedBy()).isEqualTo(DEFAULT_LOGIN);
		assertThat(userDTO.getLastModifiedDate()).isEqualTo(user.getLastModifiedDate());
		assertThat(userDTO.getAuthorities()).containsExactly(AuthoritiesConstants.USER);
		assertThat(userDTO.toString()).isNotNull();
	}

	@Test
	public void testAuthorityEquals() {
		Authority authorityA = new Authority();
		assertThat(authorityA).isEqualTo(authorityA);
		assertThat(authorityA).isNotEqualTo(null);
		assertThat(authorityA).isNotEqualTo(new Object());
		assertThat(authorityA.hashCode()).isEqualTo(0);
		assertThat(authorityA.toString()).isNotNull();

		Authority authorityB = new Authority();
		assertThat(authorityA).isEqualTo(authorityB);

		authorityB.setName(AuthoritiesConstants.ADMIN);
		assertThat(authorityA).isNotEqualTo(authorityB);

		authorityA.setName(AuthoritiesConstants.USER);
		assertThat(authorityA).isNotEqualTo(authorityB);

		authorityB.setName(AuthoritiesConstants.USER);
		assertThat(authorityA).isEqualTo(authorityB);
		assertThat(authorityA.hashCode()).isEqualTo(authorityB.hashCode());
	}

	private void assertPersistedUsers(Consumer<List<UserEntity>> userAssertion) {
		userAssertion.accept(userRepository.findAll());
	}
}
