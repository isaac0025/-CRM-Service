package com.theagilemonkeys.web.rest;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.theagilemonkeys.service.CustomerService;
import com.theagilemonkeys.service.dto.CustomerDTO;

@RestController
@RequestMapping("/api")
public class CustomerResource {

	private final Logger log = LoggerFactory.getLogger(CustomerResource.class);
	private static final String USERS_ENDPOINT = "/customers";

	@Value("${jhipster.clientApp.name}")
	private String applicationName;

	private final CustomerService customerService;

	public CustomerResource(CustomerService customerService) {
		this.customerService = customerService;
	}

	@GetMapping(USERS_ENDPOINT)
	@PreAuthorize("@securityChecker.canListCustomers(authentication)")
	public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
		List<CustomerDTO> customers = customerService.findAll();

		return new ResponseEntity<>(customers, HttpStatus.OK);
	}

	@GetMapping(USERS_ENDPOINT + "/{id}")
	@PreAuthorize("@securityChecker.canSearchCustomer(authentication)")
	public ResponseEntity<CustomerDTO> getCustomer(@PathVariable Long id) {
		log.debug("REST request to get Customer : {}", id);

		Optional<CustomerDTO> customer = customerService.findById(id);
		if (customer.isPresent()) {
			return ResponseEntity.status(HttpStatus.OK).body(customer.get());
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@PostMapping(USERS_ENDPOINT)
	@PreAuthorize("@securityChecker.canCreateCustomer(authentication)")
	public ResponseEntity<CustomerDTO> create(@Valid @RequestBody CustomerDTO customerDTO) throws URISyntaxException {
		log.debug("REST request to create Customer : {}", customerDTO.getId());
		CustomerDTO newCustomer = customerService.create(customerDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(newCustomer);
	}

	@PutMapping(USERS_ENDPOINT)
	@PreAuthorize("@securityChecker.canUpdateCustomer(authentication)")
	public ResponseEntity<CustomerDTO> update(@Valid @RequestBody CustomerDTO customerDTO) {
		log.debug("REST request to create Customer : {}", customerDTO.getId());
		Optional<CustomerDTO> updatedCustomer = customerService.update(customerDTO);

		if (updatedCustomer.isPresent()) {
			return ResponseEntity.status(HttpStatus.CREATED).body(updatedCustomer.get());
		} else {
			return ResponseEntity.badRequest().body(null);
		}
	}

	@DeleteMapping(USERS_ENDPOINT + "/{id}")
	@PreAuthorize("@securityChecker.canDeleteCustomer(authentication)")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		customerService.delete(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(USERS_ENDPOINT + "/{id}" + "/image")
	@PreAuthorize("@securityChecker.canUpdateCustomer(authentication)")
	public ResponseEntity<Void> create(@PathVariable Long id, @RequestParam("image") MultipartFile file)
			throws Exception {
		if (file.isEmpty()) {
			return ResponseEntity.badRequest().body(null);
		}

		Optional<CustomerDTO> customer = customerService.uploadCustomerImage(id, file);

		if (!customer.isPresent()) {
			return ResponseEntity.badRequest().body(null);
		}

		return new ResponseEntity<>(HttpStatus.OK);

	}
}
