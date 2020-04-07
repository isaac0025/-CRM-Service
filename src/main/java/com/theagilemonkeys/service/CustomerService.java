package com.theagilemonkeys.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.theagilemonkeys.domain.CustomerEntity;
import com.theagilemonkeys.repository.CustomerRepository;
import com.theagilemonkeys.service.dto.CustomerDTO;
import com.theagilemonkeys.service.mapper.CustomerMapper;

/**
 * Service class for managing customers.
 */
@Service
@Transactional
public class CustomerService {

	private final Logger log = LoggerFactory.getLogger(CustomerService.class);

	private final CustomerRepository customerRepository;

	private final CustomerMapper customerMapper;

	public CustomerService(CustomerRepository CustomerRepository, CustomerMapper customerMapper) {
		this.customerRepository = CustomerRepository;
		this.customerMapper = customerMapper;
	}

	public Optional<CustomerDTO> findById(Long id) {
		Optional<CustomerEntity> customer = customerRepository.findById(id);

		if (!customer.isPresent()) {
			return Optional.empty();
		}

		return Optional.of(customerMapper.customerToCustomerDTO(customer.get()));
	}

	public Optional<CustomerDTO> update(CustomerDTO CustomerDTO) {

		Optional<CustomerEntity> existingUser = customerRepository.findById(CustomerDTO.getId());

		if (!existingUser.isPresent()) {
			return Optional.empty();
		}

		CustomerEntity newUser = customerMapper.customerDTOToCustomer(CustomerDTO);
		newUser = customerRepository.saveAndFlush(newUser);
		return Optional.ofNullable(customerMapper.customerToCustomerDTO(newUser));

	}

	public void delete(Long id) {
		customerRepository.findById(id).ifPresent(user -> {
			customerRepository.delete(user);
			log.debug("Deleted User: {}", user);
		});
	}

	@Transactional(readOnly = true)
	public List<CustomerDTO> findAll() {
		List<CustomerEntity> customers = customerRepository.findAll();
		return customerMapper.customersToCustomerDTOs(customers);
	}

	public CustomerDTO create(CustomerDTO CustomerDTO) {
		CustomerEntity user = customerMapper.customerDTOToCustomer(CustomerDTO);
		user = customerRepository.saveAndFlush(user);
		return customerMapper.customerToCustomerDTO(user);
	}

}
