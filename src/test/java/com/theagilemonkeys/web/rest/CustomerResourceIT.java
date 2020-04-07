package com.theagilemonkeys.web.rest;

import static com.theagilemonkeys.web.rest.TestUtil.ID_TOKEN;
import static com.theagilemonkeys.web.rest.TestUtil.authenticationToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.util.Random;
import java.util.Set;
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
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.theagilemonkeys.CrmApp;
import com.theagilemonkeys.config.TestSecurityConfiguration;
import com.theagilemonkeys.domain.Authority;
import com.theagilemonkeys.domain.CustomerEntity;
import com.theagilemonkeys.repository.CustomerRepository;
import com.theagilemonkeys.security.AuthoritiesConstants;
import com.theagilemonkeys.service.dto.CustomerDTO;
import com.theagilemonkeys.service.mapper.CustomerMapper;

/**
 * Integration tests for the {@link CustomerResource} REST controller.
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = { CrmApp.class, TestSecurityConfiguration.class })
public class CustomerResourceIT {

	private static final Long DEFAULT_ID = 1234L;

	private static final Long CUSTOMER_ID = 1L;

	private static final String ADMIN_LOGIN = "admin";

	private static final String DEFAULT_EMAIL = "johndoe@localhost";

	private static final String DEFAULT_FIRSTNAME = "john";

	private static final String DEFAULT_LASTNAME = "doe";

	private static final String DEFAULT_LANGKEY = "en";

	private static final String ENDPOINT = "/api/customers";

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private CustomerMapper customerMapper;

	@Autowired
	private EntityManager em;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private MockMvc restCustomerMockMvc;

	@Autowired
	private WebApplicationContext context;

	private CustomerEntity customer;

	private OidcIdToken idToken;

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

		this.restCustomerMockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	/**
	 * Create a Customer.
	 *
	 * This is a static method, as tests for other entities might also need it, if
	 * they test an entity which has a required relationship to the Customer entity.
	 */
	public static CustomerEntity createEntity(EntityManager em) {
		CustomerEntity customer = new CustomerEntity();
		customer.setId(DEFAULT_ID + new Random().nextLong());
		customer.setEmail(RandomStringUtils.randomAlphabetic(5) + DEFAULT_EMAIL);
		customer.setFirstName(DEFAULT_FIRSTNAME);
		customer.setLastName(DEFAULT_LASTNAME);
		customer.setLangKey(DEFAULT_LANGKEY);
		return customer;
	}

	@BeforeEach
	public void initTest() {
		customer = createEntity(em);
		customer.setId(DEFAULT_ID);
		customer.setEmail(DEFAULT_EMAIL);
	}

	@Test
	@Transactional
	public void getAllCustomers() throws Exception {
		// Initialize the database
		customerRepository.saveAndFlush(customer);

		// Get all the customers
		restCustomerMockMvc.perform(get("/api/customers?sort=id,desc").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.[*].firstName").value(hasItem(DEFAULT_FIRSTNAME)))
				.andExpect(jsonPath("$.[*].lastName").value(hasItem(DEFAULT_LASTNAME)))
				.andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
				.andExpect(jsonPath("$.[*].langKey").value(hasItem(DEFAULT_LANGKEY)));
	}

	@Test
	@Transactional
	public void createCustomer() throws Exception {
		int databaseSizeBeforeCreate = customerRepository.findAll().size();
		CustomerDTO source = customerMapper.customerToCustomerDTO(customer);

		restCustomerMockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(TestUtil.convertObjectToJsonBytes(source))).andExpect(status().isCreated());

		List<CustomerEntity> customerList = customerRepository.findAll().stream()
				.sorted((customer1, customer2) -> customer2.getId().compareTo(customer1.getId()))
				.collect(Collectors.toList());
		assertThat(customerList).hasSize(databaseSizeBeforeCreate + 1);
		CustomerEntity testCustomer = customerList.get(0);
		assertThat(testCustomer.getId()).isEqualTo(source.getId());
		assertThat(testCustomer.getFirstName()).isEqualTo(source.getFirstName());
		assertThat(testCustomer.getLastName()).isEqualTo(source.getLastName());
		assertThat(testCustomer.getEmail()).isEqualTo(source.getEmail());

	}

	@Test
	@Transactional
	public void updateCustomer() throws Exception {

		Optional<CustomerEntity> customer = customerRepository.findById(DEFAULT_ID);
		assertThat(customer.isPresent());

		if (customer.isPresent()) {
			CustomerDTO updatedCustomerDTO = customerMapper.customerToCustomerDTO(customer.get());
			updatedCustomerDTO.setId(DEFAULT_ID);
			updatedCustomerDTO.setFirstName(DEFAULT_FIRSTNAME);
			updatedCustomerDTO.setLastName(DEFAULT_LASTNAME);
			updatedCustomerDTO.setEmail(DEFAULT_EMAIL);

			restCustomerMockMvc
					.perform(put(ENDPOINT).contentType(MediaType.APPLICATION_JSON_VALUE)
							.content(TestUtil.convertObjectToJsonBytes(updatedCustomerDTO)))
					.andExpect(status().isCreated());

			Optional<CustomerEntity> updatedCustomer = customerRepository.findById(customer.get().getId());
			assertThat(updatedCustomer.isPresent());
			if (updatedCustomer.isPresent()) {
				assertThat(updatedCustomer.get().getId()).isEqualTo(DEFAULT_ID);
				assertThat(updatedCustomer.get().getFirstName()).isEqualTo(updatedCustomerDTO.getFirstName());
				assertThat(updatedCustomer.get().getLastName()).isEqualTo(updatedCustomerDTO.getLastName());
				assertThat(updatedCustomer.get().getEmail()).isEqualTo(updatedCustomerDTO.getEmail());
			}

		}

	}

	@Test
	@Transactional
	public void deleteCustomer() throws Exception {

		customerRepository.saveAndFlush(customer);

		assertThat(customerRepository.findById(CUSTOMER_ID).isPresent());

		restCustomerMockMvc.perform(delete(ENDPOINT + "/{login}", CUSTOMER_ID)).andExpect(status().isOk());

		assertThat(!customerRepository.findById(CUSTOMER_ID).isPresent());

	}

	@Test
	@Transactional
	public void getCustomer() throws Exception {
		// Initialize the database
		customerRepository.saveAndFlush(customer);

		// Get the customer
		restCustomerMockMvc.perform(get(ENDPOINT + "/{login}", customer.getId())).andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.id").value(customer.getId()))
				.andExpect(jsonPath("$.firstName").value(DEFAULT_FIRSTNAME))
				.andExpect(jsonPath("$.lastName").value(DEFAULT_LASTNAME))
				.andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
				.andExpect(jsonPath("$.langKey").value(DEFAULT_LANGKEY));
	}

	@Test
	@Transactional
	public void getNonExistingCustomer() throws Exception {
		restCustomerMockMvc.perform(get("/api/customers/" + new Random().nextLong())).andExpect(status().isNotFound());
	}

	@Test
	@Transactional
	public void testCustomerEquals() throws Exception {
		TestUtil.equalsVerifier(CustomerEntity.class);
		CustomerEntity customer1 = new CustomerEntity();
		customer1.setId(10L);
		CustomerEntity customer2 = new CustomerEntity();
		customer2.setId(customer1.getId());
		assertThat(customer1).isEqualTo(customer2);
		customer2.setId(12L);
		assertThat(customer1).isNotEqualTo(customer2);
		customer1.setId(11L);
		assertThat(customer1).isNotEqualTo(customer2);
	}

	@Test
	public void testCustomerDTOtoCustomer() {
		CustomerDTO customerDTO = new CustomerDTO();
		customerDTO.setId(DEFAULT_ID);
		customerDTO.setFirstName(DEFAULT_FIRSTNAME);
		customerDTO.setLastName(DEFAULT_LASTNAME);
		customerDTO.setEmail(DEFAULT_EMAIL);
		customerDTO.setLangKey(DEFAULT_LANGKEY);
		customerDTO.setCreatedBy(ADMIN_LOGIN);
		customerDTO.setLastModifiedBy(ADMIN_LOGIN);

		CustomerEntity customer = customerMapper.customerDTOToCustomer(customerDTO);
		assertThat(customer.getId()).isEqualTo(DEFAULT_ID);
		assertThat(customer.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
		assertThat(customer.getLastName()).isEqualTo(DEFAULT_LASTNAME);
		assertThat(customer.getEmail()).isEqualTo(DEFAULT_EMAIL);
		assertThat(customer.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
		assertThat(customer.getCreatedBy()).isNull();
		assertThat(customer.getCreatedDate()).isNotNull();
		assertThat(customer.getLastModifiedBy()).isNull();
		assertThat(customer.getLastModifiedDate()).isNotNull();
	}

	@Test
	public void testCustomerToCustomerDTO() {
		customer.setCreatedBy(ADMIN_LOGIN);
		customer.setCreatedDate(Instant.now());
		customer.setLastModifiedBy(ADMIN_LOGIN);
		customer.setLastModifiedDate(Instant.now());
		Set<Authority> authorities = new HashSet<>();
		Authority authority = new Authority();
		authority.setName(AuthoritiesConstants.USER);
		authorities.add(authority);

		CustomerDTO customerDTO = customerMapper.customerToCustomerDTO(customer);

		assertThat(customerDTO.getId()).isEqualTo(DEFAULT_ID);
		assertThat(customerDTO.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
		assertThat(customerDTO.getLastName()).isEqualTo(DEFAULT_LASTNAME);
		assertThat(customerDTO.getEmail()).isEqualTo(DEFAULT_EMAIL);
		assertThat(customerDTO.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
		assertThat(customerDTO.getCreatedBy()).isEqualTo(ADMIN_LOGIN);
		assertThat(customerDTO.getCreatedDate()).isEqualTo(customer.getCreatedDate());
		assertThat(customerDTO.getLastModifiedBy()).isEqualTo(ADMIN_LOGIN);
		assertThat(customerDTO.getLastModifiedDate()).isEqualTo(customer.getLastModifiedDate());
		assertThat(customerDTO.toString()).isNotNull();
	}

}
