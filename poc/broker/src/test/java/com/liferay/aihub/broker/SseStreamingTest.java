package com.liferay.aihub.broker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;

/**
 * Proves the broker does not buffer the event stream.
 *
 * <p>
 * The mock sidecar spaces its frames 300 milliseconds apart. If the broker
 * buffered, every chunk would land at roughly the same instant at the end of
 * the response. The assertion is therefore on the spread of arrival times, not
 * on the content.
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SseStreamingTest {

	@BeforeAll
	public static void setUpClass() throws Exception {
		_mockSidecarServer = new MockSidecarServer(0, 300L);
	}

	@AfterAll
	public static void tearDownClass() {
		if (_mockSidecarServer != null) {
			_mockSidecarServer.close();
		}
	}

	@DynamicPropertySource
	public static void configure(
		DynamicPropertyRegistry dynamicPropertyRegistry) {

		dynamicPropertyRegistry.add("broker.auth.enabled", () -> "false");
		dynamicPropertyRegistry.add(
			"broker.sidecar.base-url",
			() -> "http://localhost:" + _mockSidecarServer.getPort());
	}

	@Test
	public void testFramesArriveIncrementally() {
		WebClient webClient = WebClient.create("http://localhost:" + _port);

		long start = System.nanoTime();

		List<Long> offsets = new ArrayList<>();
		List<String> chunks = new ArrayList<>();

		Flux<String> flux = webClient.get(
		).uri(
			"/api/sessions/abc/stream"
		).accept(
			MediaType.TEXT_EVENT_STREAM
		).retrieve(
		).bodyToFlux(
			String.class
		);

		flux.doOnNext(
			chunk -> {
				offsets.add((System.nanoTime() - start) / 1_000_000L);
				chunks.add(chunk);
			}
		).blockLast(
			Duration.ofSeconds(30)
		);

		System.out.println("Chunk arrival offsets in milliseconds: " + offsets);

		Assertions.assertTrue(
			chunks.size() >= 5,
			"Expected several chunks but received " + chunks.size());

		long first = offsets.get(0);
		long last = offsets.get(offsets.size() - 1);

		Assertions.assertTrue(
			(last - first) > 1500,
			"Frames arrived within " + (last - first) +
				" milliseconds of each other, which means the stream was " +
					"buffered");

		String joined = String.join("", chunks);

		Assertions.assertTrue(
			joined.contains("\"state\":\"deploying\""),
			"Deploy status frame missing from " + joined);
		Assertions.assertTrue(
			joined.contains("costUsd"), "Result frame missing from " + joined);
	}

	private static MockSidecarServer _mockSidecarServer;

	@LocalServerPort
	private int _port;

}
