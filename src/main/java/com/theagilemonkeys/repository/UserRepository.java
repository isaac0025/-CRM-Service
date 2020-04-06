package com.theagilemonkeys.repository;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.theagilemonkeys.domain.UserEntity;

/**
 * Spring Data JPA repository for the {@link UserEntity} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

	String USERS_BY_LOGIN_CACHE = "usersByLogin";

	String USERS_BY_EMAIL_CACHE = "usersByEmail";

	Optional<UserEntity> findOneByEmailIgnoreCase(String email);

	Optional<UserEntity> findOneByLogin(String login);

	@EntityGraph(attributePaths = "authorities")
	Optional<UserEntity> findOneWithAuthoritiesById(Long id);

	@EntityGraph(attributePaths = "authorities")
	@Cacheable(cacheNames = USERS_BY_LOGIN_CACHE)
	Optional<UserEntity> findOneWithAuthoritiesByLogin(String login);

	@EntityGraph(attributePaths = "authorities")
	@Cacheable(cacheNames = USERS_BY_EMAIL_CACHE)
	Optional<UserEntity> findOneWithAuthoritiesByEmailIgnoreCase(String email);

	Page<UserEntity> findAllByLoginNot(Pageable pageable, String login);
}
