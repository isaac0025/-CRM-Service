package com.theagilemonkeys.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Crm.
 * <p>
 * Properties are configured in the {@code application.yml} file. See
 * {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

	private final Minio minio = new Minio();

	public Minio getMinio() {
		return minio;
	}

	public static class Minio {

		private String endpoint;

		private String accessKey;

		private String secretKey;

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public String getAccessKey() {
			return accessKey;
		}

		public void setAccessKey(String accessKey) {
			this.accessKey = accessKey;
		}

		public String getSecretKey() {
			return secretKey;
		}

		public void setSecretKey(String secretKey) {
			this.secretKey = secretKey;
		}

	}

}
