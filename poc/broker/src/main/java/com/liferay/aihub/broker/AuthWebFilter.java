package com.liferay.aihub.broker;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Enforces the bearer token requirement of the contract.
 *
 * <p>
 * <code>GET /api/health</code> and cross origin preflight requests are exempt.
 * Setting <code>broker.auth.enabled=false</code> disables the filter entirely,
 * which is the supported way to demonstrate the POC without a running DXP.
 * </p>
 */
@Component
@Order(0)
public class AuthWebFilter implements WebFilter {

	public AuthWebFilter(
		BrokerProperties brokerProperties, DxpTokenValidator dxpTokenValidator) {

		_auth = brokerProperties.getAuth();
		_dxpTokenValidator = dxpTokenValidator;

		if (!_auth.isEnabled()) {
			_log.warn(
				"Authentication is disabled. Every request to the broker is " +
					"forwarded to the sidecar without a DXP check.");
		}
	}

	@Override
	public Mono<Void> filter(
		ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {

		ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();

		if (!_auth.isEnabled() ||
			CorsUtils.isPreFlightRequest(serverHttpRequest) ||
			_isExempt(serverHttpRequest)) {

			return webFilterChain.filter(serverWebExchange);
		}

		HttpHeaders httpHeaders = serverHttpRequest.getHeaders();

		String authorization = httpHeaders.getFirst(HttpHeaders.AUTHORIZATION);

		if ((authorization == null) ||
			!authorization.regionMatches(true, 0, _BEARER, 0, _BEARER.length())) {

			return _unauthorized(
				serverWebExchange, "Missing bearer token");
		}

		String token = authorization.substring(_BEARER.length()).trim();

		if (token.isEmpty()) {
			return _unauthorized(serverWebExchange, "Empty bearer token");
		}

		return _dxpTokenValidator.validate(
			token
		).flatMap(
			valid -> {
				if (valid) {
					return webFilterChain.filter(serverWebExchange);
				}

				return _unauthorized(
					serverWebExchange, "Token rejected by DXP");
			}
		);
	}

	private boolean _isExempt(ServerHttpRequest serverHttpRequest) {
		String path = String.valueOf(serverHttpRequest.getPath());

		if (_HEALTH_PATH.equals(path) || !path.startsWith("/api/")) {
			return true;
		}

		return false;
	}

	private Mono<Void> _unauthorized(
		ServerWebExchange serverWebExchange, String message) {

		ServerHttpResponse serverHttpResponse = serverWebExchange.getResponse();

		serverHttpResponse.setStatusCode(HttpStatus.UNAUTHORIZED);

		HttpHeaders httpHeaders = serverHttpResponse.getHeaders();

		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		httpHeaders.set(
			HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"liferay-dxp\"");

		DataBufferFactory dataBufferFactory =
			serverHttpResponse.bufferFactory();

		DataBuffer dataBuffer = dataBufferFactory.wrap(
			("{\"message\":\"" + message + "\"}").getBytes(
				StandardCharsets.UTF_8));

		return serverHttpResponse.writeWith(Mono.just(dataBuffer));
	}

	private static final String _BEARER = "Bearer ";

	private static final String _HEALTH_PATH = "/api/health";

	private static final Logger _log = LoggerFactory.getLogger(
		AuthWebFilter.class);

	private final BrokerProperties.Auth _auth;
	private final DxpTokenValidator _dxpTokenValidator;

}
