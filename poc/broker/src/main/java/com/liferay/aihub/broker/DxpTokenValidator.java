package com.liferay.aihub.broker;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.time.Duration;
import java.time.Instant;

import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

/**
 * Validates a bearer token by presenting it to DXP. A status of 200 from
 * <code>/o/headless-admin-user/v1.0/my-user-account</code> is the only accepted
 * proof. Positive answers are cached for the configured window, keyed by a
 * SHA-256 hash of the token so the raw token is never held in the cache.
 */
@Component
public class DxpTokenValidator {

	public DxpTokenValidator(
		BrokerProperties brokerProperties,
		@Qualifier("dxpWebClient") WebClient dxpWebClient) {

		_auth = brokerProperties.getAuth();
		_dxpWebClient = dxpWebClient;
	}

	public Mono<Boolean> validate(String token) {
		String key = hash(token);

		Instant expiresAt = _cache.get(key);
		Instant now = Instant.now();

		if ((expiresAt != null) && expiresAt.isAfter(now)) {
			return Mono.just(Boolean.TRUE);
		}

		if (expiresAt != null) {
			_cache.remove(key, expiresAt);
		}

		Duration timeout = _auth.getTimeout();

		return _dxpWebClient.get(
		).uri(
			_auth.getValidationPath()
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + token
		).exchangeToMono(
			clientResponse -> clientResponse.releaseBody(
			).thenReturn(
				clientResponse.statusCode()
			)
		).timeout(
			timeout
		).map(
			httpStatusCode -> {
				if (httpStatusCode.value() == 200) {
					_cache.put(key, Instant.now().plus(_auth.getCacheTtl()));

					return Boolean.TRUE;
				}

				return Boolean.FALSE;
			}
		).onErrorResume(
			throwable -> {
				_log.warn(
					"Unable to validate token against DXP at {}{}: {}",
					_auth.getDxpBaseUrl(), _auth.getValidationPath(),
					throwable.getMessage());

				return Mono.just(Boolean.FALSE);
			}
		);
	}

	protected static String hash(String token) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

			HexFormat hexFormat = HexFormat.of();

			return hexFormat.formatHex(
				messageDigest.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException noSuchAlgorithmException) {
			throw new IllegalStateException(noSuchAlgorithmException);
		}
	}

	private static final Logger _log = LoggerFactory.getLogger(
		DxpTokenValidator.class);

	private final BrokerProperties.Auth _auth;
	private final Map<String, Instant> _cache = new ConcurrentHashMap<>();
	private final WebClient _dxpWebClient;

}
