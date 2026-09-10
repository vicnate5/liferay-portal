package com.liferay.aihub.broker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * The chat element is served from DXP on 8080 and calls the broker on 8091, so
 * a preflight has to succeed even though the broker demands a token.
 */
@SpringBootTest(
	properties = {
		"broker.auth.enabled=true", "broker.cors.allowed-origin=http://localhost:8080"
	},
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CorsTest {

	@Test
	public void testPreflightIsAllowedWithoutToken() {
		_webTestClient.options(
		).uri(
			"/api/sessions"
		).header(
			HttpHeaders.ORIGIN, "http://localhost:8080"
		).header(
			HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"
		).header(
			HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"
		).exchange(
		).expectStatus(
		).isOk(
		).expectHeader(
		).valueEquals(
			HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8080"
		).expectHeader(
		).valueEquals(
			HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"
		);
	}

	@Test
	public void testUnknownOriginIsRejected() {
		_webTestClient.options(
		).uri(
			"/api/sessions"
		).header(
			HttpHeaders.ORIGIN, "http://evil.example.com"
		).header(
			HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"
		).exchange(
		).expectStatus(
		).isForbidden(
		);
	}

	@Autowired
	private WebTestClient _webTestClient;

}
