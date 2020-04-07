package com.theagilemonkeys.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.theagilemonkeys.config.ApplicationProperties;
import com.theagilemonkeys.domain.CustomerEntity;
import com.theagilemonkeys.repository.CustomerRepository;
import com.theagilemonkeys.service.dto.CustomerDTO;
import com.theagilemonkeys.service.mapper.CustomerMapper;

import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import liquibase.util.file.FilenameUtils;

/**
 * Service class for managing customers.
 */
@Service
@Transactional
public class CustomerService {

	private final Logger log = LoggerFactory.getLogger(CustomerService.class);

	private final CustomerRepository customerRepository;

	private final CustomerMapper customerMapper;

	private final ApplicationProperties applicationProperties;

	private static final String MINIO_BUCKET_NAME = "customersimages";

	public CustomerService(CustomerRepository CustomerRepository, CustomerMapper customerMapper,
			ApplicationProperties applicationProperties) {
		this.customerRepository = CustomerRepository;
		this.customerMapper = customerMapper;
		this.applicationProperties = applicationProperties;
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

	public Optional<CustomerDTO> uploadCustomerImage(Long id, MultipartFile file) throws Exception {
		Optional<CustomerEntity> customer = customerRepository.findById(id);
		if (!customer.isPresent()) {
			return Optional.empty();
		}

		try {
			String filenameExtension = FilenameUtils.getExtension(file.getOriginalFilename());
			String filename = id.toString() + "." + filenameExtension;
			String imageUrl = uploadImage(filename, file);
			customer.get().setImageUrl(imageUrl);
			CustomerEntity updatedCustomer = customerRepository.saveAndFlush(customer.get());
			return Optional.of(customerMapper.customerToCustomerDTO(updatedCustomer));
		} catch (Exception e) {
			log.error(e.getMessage());
			throw e;
		}

	}

	private String uploadImage(String filename, MultipartFile file) throws Exception {
		MinioClient minioClient = new MinioClient(applicationProperties.getMinio().getEndpoint(),
				applicationProperties.getMinio().getAccessKey(), applicationProperties.getMinio().getSecretKey());

		boolean isExist = minioClient.bucketExists(MINIO_BUCKET_NAME);
		if (!isExist) {
			minioClient.makeBucket(MINIO_BUCKET_NAME);
		}
		minioClient.putObject(MINIO_BUCKET_NAME, filename, file.getInputStream(),
				new PutObjectOptions(file.getInputStream().available(), -1));

		return minioClient.getObjectUrl(MINIO_BUCKET_NAME, filename);

	}

}
