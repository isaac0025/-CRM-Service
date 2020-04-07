package com.theagilemonkeys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.theagilemonkeys.domain.CustomerEntity;

/**
 * Spring Data JPA repository for the {@link CustomerEntity} entity.
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

}
