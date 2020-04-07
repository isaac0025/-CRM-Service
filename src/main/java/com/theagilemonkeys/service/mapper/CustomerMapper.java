package com.theagilemonkeys.service.mapper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.theagilemonkeys.domain.CustomerEntity;
import com.theagilemonkeys.service.dto.CustomerDTO;

/**
 * Mapper for the entity {@link CustomerEntity} and its DTO called
 * {@link CustomerDTO}.
 */
@Service
public class CustomerMapper {

	public List<CustomerDTO> customersToCustomerDTOs(List<CustomerEntity> customers) {
		return customers.stream().filter(Objects::nonNull).map(this::customerToCustomerDTO).collect(Collectors.toList());
	}

	public CustomerDTO customerToCustomerDTO(CustomerEntity user) {
		return new CustomerDTO(user);
	}

	public List<CustomerEntity> customerDTOsToCustomers(List<CustomerDTO> CustomerDTOs) {
		return CustomerDTOs.stream().filter(Objects::nonNull).map(this::customerDTOToCustomer).collect(Collectors.toList());
	}

	public CustomerEntity customerDTOToCustomer(CustomerDTO CustomerDTO) {
		if (CustomerDTO == null) {
			return null;
		} else {
			CustomerEntity customer = new CustomerEntity();
			customer.setId(CustomerDTO.getId());
			customer.setImageUrl(CustomerDTO.getImageUrl());
			customer.setFirstName(CustomerDTO.getFirstName());
			customer.setLastName(CustomerDTO.getLastName());
			customer.setEmail(CustomerDTO.getEmail());
			customer.setLangKey(CustomerDTO.getLangKey());
			return customer;
		}
	}

	public CustomerEntity customerFromId(Long id) {
		if (id == null) {
			return null;
		}
		CustomerEntity customer = new CustomerEntity();
		customer.setId(id);
		return customer;
	}
}
