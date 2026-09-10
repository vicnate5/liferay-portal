package com.liferay.aihub.broker;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration surface of the broker. Every value has a default that matches
 * the fixed ports of the integration contract.
 */
@ConfigurationProperties(prefix = "broker")
public class BrokerProperties {

	public Auth getAuth() {
		return _auth;
	}

	public Cors getCors() {
		return _cors;
	}

	public Sidecar getSidecar() {
		return _sidecar;
	}

	public static class Auth {

		public Duration getCacheTtl() {
			return _cacheTtl;
		}

		public String getDxpBaseUrl() {
			return _dxpBaseUrl;
		}

		public Duration getTimeout() {
			return _timeout;
		}

		public String getValidationPath() {
			return _validationPath;
		}

		public boolean isEnabled() {
			return _enabled;
		}

		public void setCacheTtl(Duration cacheTtl) {
			_cacheTtl = cacheTtl;
		}

		public void setDxpBaseUrl(String dxpBaseUrl) {
			_dxpBaseUrl = _stripTrailingSlash(dxpBaseUrl);
		}

		public void setEnabled(boolean enabled) {
			_enabled = enabled;
		}

		public void setTimeout(Duration timeout) {
			_timeout = timeout;
		}

		public void setValidationPath(String validationPath) {
			_validationPath = validationPath;
		}

		private Duration _cacheTtl = Duration.ofSeconds(60);
		private String _dxpBaseUrl = "http://localhost:8080";
		private boolean _enabled = true;
		private Duration _timeout = Duration.ofSeconds(10);
		private String _validationPath =
			"/o/headless-admin-user/v1.0/my-user-account";

	}

	public static class Cors {

		public String getAllowedOrigin() {
			return _allowedOrigin;
		}

		public void setAllowedOrigin(String allowedOrigin) {
			_allowedOrigin = allowedOrigin;
		}

		private String _allowedOrigin = "http://localhost:8080";

	}

	public static class Sidecar {

		public String getBaseUrl() {
			return _baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			_baseUrl = _stripTrailingSlash(baseUrl);
		}

		private String _baseUrl = "http://localhost:8090";

	}

	private static String _stripTrailingSlash(String value) {
		if ((value != null) && value.endsWith("/")) {
			return value.substring(0, value.length() - 1);
		}

		return value;
	}

	private final Auth _auth = new Auth();
	private final Cors _cors = new Cors();
	private final Sidecar _sidecar = new Sidecar();

}
