package com.liferay.aihub.broker;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;

import java.net.InetSocketAddress;

import java.nio.charset.StandardCharsets;

import java.time.Instant;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Throwaway stand in for the Track A sidecar.
 *
 * <p>
 * It exists only so this track can be verified while Track A is still being
 * built. It implements the contract paths with fixed answers and, most
 * importantly, emits the server sent event frames one at a time with a real
 * delay between them, which is the behaviour the broker has to preserve.
 * </p>
 *
 * <p>
 * Run it with <code>./gradlew runMockSidecar</code>.
 * </p>
 */
public class MockSidecarServer implements AutoCloseable {

	public static void main(String[] args) throws Exception {
		int port = 8090;

		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}

		MockSidecarServer mockSidecarServer = new MockSidecarServer(port);

		System.out.println(
			"Mock sidecar listening on http://localhost:" +
				mockSidecarServer.getPort());
		System.out.println("Press Ctrl+C to stop");

		Thread.currentThread().join();
	}

	public MockSidecarServer(int port) throws IOException {
		this(port, 400L);
	}

	public MockSidecarServer(int port, long frameDelayMillis) throws IOException {
		_frameDelayMillis = frameDelayMillis;

		_httpServer = HttpServer.create(new InetSocketAddress(port), 0);

		_httpServer.createContext("/api/", this::_handle);
		_httpServer.setExecutor(Executors.newCachedThreadPool());
		_httpServer.start();
	}

	@Override
	public void close() {
		_httpServer.stop(0);
	}

	public int getPort() {
		InetSocketAddress inetSocketAddress = _httpServer.getAddress();

		return inetSocketAddress.getPort();
	}

	private void _handle(HttpExchange httpExchange) throws IOException {
		java.net.URI uri = httpExchange.getRequestURI();

		String path = uri.getPath();
		String method = httpExchange.getRequestMethod();

		try {
			if (path.equals("/api/health")) {
				_json(
					httpExchange, 200,
					"{\"status\":\"ok\",\"workspace\":\"/workspace\"," +
						"\"agent\":\"claude-agent-sdk\",\"version\":" +
							"\"0.3.263\"}");

				return;
			}

			if (path.equals("/api/sessions") && method.equals("POST")) {
				httpExchange.getRequestBody().readAllBytes();

				_json(
					httpExchange, 201,
					"{\"sessionId\":\"" + UUID.randomUUID() +
						"\",\"title\":\"mock\",\"createdAt\":\"" +
							Instant.now() + "\"}");

				return;
			}

			if (path.equals("/api/sessions") && method.equals("GET")) {
				_json(
					httpExchange, 200,
					"[{\"sessionId\":\"" + _SESSION_ID +
						"\",\"title\":\"mock\",\"createdAt\":\"" +
							Instant.now() + "\",\"state\":\"idle\"}]");

				return;
			}

			if (path.endsWith("/stream") && method.equals("GET")) {
				_stream(httpExchange);

				return;
			}

			if (path.endsWith("/messages") && method.equals("POST")) {
				httpExchange.getRequestBody().readAllBytes();

				httpExchange.sendResponseHeaders(202, -1);
				httpExchange.close();

				return;
			}

			if (path.endsWith("/messages") && method.equals("GET")) {
				_json(httpExchange, 200, "[]");

				return;
			}

			if (path.endsWith("/interrupt") && method.equals("POST")) {
				httpExchange.getRequestBody().readAllBytes();

				httpExchange.sendResponseHeaders(204, -1);
				httpExchange.close();

				return;
			}

			_json(httpExchange, 404, "{\"message\":\"Not found\"}");
		}
		catch (IOException ioException) {
			httpExchange.close();

			throw ioException;
		}
	}

	private void _json(HttpExchange httpExchange, int status, String body)
		throws IOException {

		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

		com.sun.net.httpserver.Headers headers =
			httpExchange.getResponseHeaders();

		headers.set("Content-Type", "application/json");

		httpExchange.sendResponseHeaders(status, bytes.length);

		try (OutputStream outputStream = httpExchange.getResponseBody()) {
			outputStream.write(bytes);
		}
	}

	/**
	 * Emits the contract frames with a delay between each one and flushes after
	 * every write, so a buffering proxy in front of it is immediately visible.
	 */
	private void _stream(HttpExchange httpExchange) throws IOException {
		com.sun.net.httpserver.Headers headers =
			httpExchange.getResponseHeaders();

		headers.set("Cache-Control", "no-cache");
		headers.set("Content-Type", "text/event-stream");

		// A response length of zero selects chunked transfer encoding.

		httpExchange.sendResponseHeaders(200, 0);

		List<Map.Entry<String, String>> frames = List.of(
			Map.entry("status", "{\"state\":\"thinking\"}"),
			Map.entry("assistant", "{\"text\":\"Reading the workspace. \"}"),
			Map.entry(
				"tool_use",
				"{\"id\":\"t1\",\"name\":\"Bash\",\"input\":{\"command\":" +
					"\"./gradlew deploy\"}}"),
			Map.entry("status", "{\"state\":\"working\"}"),
			Map.entry(
				"tool_result",
				"{\"id\":\"t1\",\"ok\":true,\"preview\":\"BUILD SUCCESSFUL\"}"),
			Map.entry("status", "{\"state\":\"deploying\"}"),
			Map.entry(
				"deploy",
				"{\"artifact\":\"sample.jar\",\"state\":\"started\",\"path\":" +
					"\"/liferay/deploy/sample.jar\"}"),
			Map.entry(
				"deploy",
				"{\"artifact\":\"sample.jar\",\"state\":\"copied\",\"path\":" +
					"\"/liferay/deploy/sample.jar\"}"),
			Map.entry("assistant", "{\"text\":\"Deployed.\"}"),
			Map.entry("status", "{\"state\":\"idle\"}"),
			Map.entry(
				"result",
				"{\"ok\":true,\"durationMs\":4200,\"costUsd\":0.031," +
					"\"turns\":3}"));

		try (OutputStream outputStream = httpExchange.getResponseBody()) {
			_write(outputStream, ":ping\n\n");

			for (Map.Entry<String, String> frame : frames) {
				try {
					Thread.sleep(_frameDelayMillis);
				}
				catch (InterruptedException interruptedException) {
					Thread.currentThread().interrupt();

					return;
				}

				_write(
					outputStream,
					"event: " + frame.getKey() + "\ndata: " +
						frame.getValue() + "\n\n");
			}
		}
		catch (IOException ioException) {

			// The client hung up, which is normal for a stream.

		}
	}

	private void _write(OutputStream outputStream, String text)
		throws IOException {

		outputStream.write(text.getBytes(StandardCharsets.UTF_8));

		outputStream.flush();
	}

	private static final String _SESSION_ID =
		"11111111-2222-3333-4444-555555555555";

	private final long _frameDelayMillis;
	private final HttpServer _httpServer;

}
