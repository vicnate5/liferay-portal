package com.liferay.aihub.broker;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;

import java.net.InetSocketAddress;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Covers the authentication rules of the contract: health is open, everything
 * else demands a bearer token that DXP accepts.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthTest {

	@BeforeAll
	public static void setUpClass() throws Exception {
		_mockSidecarServer = new MockSidecarServer(0, 50L);

		_mockDxpServer = HttpServer.create(new InetSocketAddress(0), 0);

		_mockDxpServer.createContext(
			"/o/headless-admin-user/v1.0/my-user-account",
			AuthTest::_handleMyUserAccount);
		_mockDxpServer.start();
	}

	@AfterAll
	public static void tearDownClass() {
		if (_mockSidecarServer != null) {
			_mockSidecarServer.close();
		}

		if (_mockDxpServer != null) {
			_mockDxpServer.stop(0);
		}
	}

	@DynamicPropertySource
	public static void configure(
		DynamicPropertyRegistry dynamicPropertyRegistry) {

		InetSocketAddress inetSocketAddress = _mockDxpServer.getAddress();

		dynamicPropertyRegistry.add("broker.auth.enabled", () -> "true");
		dynamicPropertyRegistry.add(
			"broker.auth.dxp-base-url",
			() -> "http://localhost:" + inetSocketAddress.getPort());
		dynamicPropertyRegistry.add(
			"broker.sidecar.base-url",
			() -> "http://localhost:" + _mockSidecarServer.getPort());
	}

	@Test
	public void testAcceptedTokenReachesSidecar() {
		_webTestClient.get(
		).uri(
			"/api/sessions"
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + _GOOD_TOKEN
		).exchange(
		).expectStatus(
		).isOk(
		).expectBody(
		).jsonPath(
			"$[0].state"
		).isEqualTo(
			"idle"
		);
	}

	@Test
	public void testHealthIsExemptFromAuth() {
		_webTestClient.get(
		).uri(
			"/api/health"
		).exchange(
		).expectStatus(
		).isOk(
		).expectBody(
		).jsonPath(
			"$.status"
		).isEqualTo(
			"ok"
		);
	}

	@Test
	public void testNoTokenIsUnauthorized() {
		_webTestClient.get(
		).uri(
			"/api/sessions"
		).exchange(
		).expectStatus(
		).isUnauthorized(
		);
	}

	@Test
	public void testRejectedTokenIsUnauthorized() {
		_webTestClient.post(
		).uri(
			"/api/sessions"
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"
		).exchange(
		).expectStatus(
		).isUnauthorized(
		);
	}

	@Test
	public void testStreamWithoutTokenIsUnauthorized() {
		_webTestClient.get(
		).uri(
			"/api/sessions/abc/stream"
		).exchange(
		).expectStatus(
		).isUnauthorized(
		);
	}

	private static void _handleMyUserAccount(HttpExchange httpExchange)
		throws IOException {

		com.sun.net.httpserver.Headers headers =
			httpExchange.getRequestHeaders();

		String authorization = headers.getFirst("Authorization");

		boolean ok = "Bearer ".concat(_GOOD_TOKEN).equals(authorization);

		byte[] bytes = null;

		if (ok) {
			bytes = "{\"id\":20124}".getBytes(StandardCharsets.UTF_8);
		}
		else {
			bytes = "{\"status\":\"UNAUTHORIZED\"}".getBytes(
				StandardCharsets.UTF_8);
		}

		com.sun.net.httpserver.Headers responseHeaders =
			httpExchange.getResponseHeaders();

		responseHeaders.set("Content-Type", "application/json");

		httpExchange.sendResponseHeaders(ok ? 200 : 401, bytes.length);

		try (OutputStream outputStream = httpExchange.getResponseBody()) {
			outputStream.write(bytes);
		}
	}

	private static final String _GOOD_TOKEN = "valid-dxp-token";

	private static HttpServer _mockDxpServer;
	private static MockSidecarServer _mockSidecarServer;

	@Autowired
	private WebTestClient _webTestClient;

}
