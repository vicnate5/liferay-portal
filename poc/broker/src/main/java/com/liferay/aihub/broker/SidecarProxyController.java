package com.liferay.aihub.broker;

import java.net.URI;

import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Forwards the contract API from the broker to the sidecar.
 *
 * <p>
 * Two handlers are declared. The stream handler is matched first because its
 * pattern is more specific than the catch all, and it is the only handler that
 * writes with an explicit flush per frame.
 * </p>
 */
@RestController
public class SidecarProxyController {

	public SidecarProxyController(
		BrokerProperties brokerProperties,
		@Qualifier("sidecarWebClient") WebClient sidecarWebClient) {

		_sidecar = brokerProperties.getSidecar();
		_sidecarWebClient = sidecarWebClient;
	}

	/**
	 * Catch all proxy for every non streaming path of the contract:
	 * <code>/api/health</code>, <code>/api/sessions</code>,
	 * <code>/api/sessions/{id}/messages</code> and
	 * <code>/api/sessions/{id}/interrupt</code>.
	 */
	@RequestMapping("/api/**")
	public Mono<Void> proxy(ServerWebExchange serverWebExchange) {
		ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
		ServerHttpResponse serverHttpResponse = serverWebExchange.getResponse();

		URI uri = _targetURI(serverHttpRequest);

		HttpMethod httpMethod = serverHttpRequest.getMethod();

		if (_log.isDebugEnabled()) {
			_log.debug("Proxying {} {}", httpMethod, uri);
		}

		WebClient.RequestBodySpec requestBodySpec = _sidecarWebClient.method(
			httpMethod
		).uri(
			uri
		).headers(
			httpHeaders -> _copyRequestHeaders(serverHttpRequest, httpHeaders)
		);

		WebClient.RequestHeadersSpec<?> requestHeadersSpec = requestBodySpec;

		// A body is only forwarded for methods that carry one. Attaching an
		// empty publisher to a GET makes the client announce a chunked body,
		// which some servers reject.

		if (_CARRIES_BODY.contains(httpMethod)) {
			requestHeadersSpec = requestBodySpec.body(
				BodyInserters.fromDataBuffers(serverHttpRequest.getBody()));
		}

		return requestHeadersSpec.exchangeToMono(
			clientResponse -> {
				HttpStatusCode httpStatusCode = clientResponse.statusCode();

				serverHttpResponse.setStatusCode(httpStatusCode);

				_copyResponseHeaders(clientResponse, serverHttpResponse);

				if (_isBodyless(httpStatusCode)) {
					return clientResponse.releaseBody(
					).then(
						serverHttpResponse.setComplete()
					);
				}

				return serverHttpResponse.writeWith(
					clientResponse.bodyToFlux(DataBuffer.class));
			}
		).onErrorResume(
			throwable -> _badGateway(serverHttpResponse, throwable)
		);
	}

	/**
	 * Server sent event passthrough.
	 *
	 * <p>
	 * The body is taken as raw {@link DataBuffer} instances and handed to
	 * {@link ServerHttpResponse#writeAndFlushWith} with each buffer wrapped in
	 * its own inner publisher. That wrapping is the whole trick. It instructs
	 * the reactive HTTP layer to flush after every single chunk instead of
	 * letting the transport decide when to drain, which is what makes the
	 * frames arrive at the browser as they are produced.
	 * </p>
	 *
	 * <p>
	 * Nothing here parses or re-serialises the stream, so heartbeat comments
	 * and unknown event names pass through untouched.
	 * </p>
	 */
	@GetMapping("/api/sessions/{sessionId}/stream")
	public Mono<Void> stream(
		@PathVariable String sessionId, ServerWebExchange serverWebExchange) {

		ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
		ServerHttpResponse serverHttpResponse = serverWebExchange.getResponse();

		URI uri = _targetURI(serverHttpRequest);

		if (_log.isDebugEnabled()) {
			_log.debug("Opening stream for session {} at {}", sessionId, uri);
		}

		return _sidecarWebClient.get(
		).uri(
			uri
		).accept(
			MediaType.TEXT_EVENT_STREAM
		).headers(
			httpHeaders -> _copyRequestHeaders(serverHttpRequest, httpHeaders)
		).exchangeToMono(
			clientResponse -> {
				HttpStatusCode httpStatusCode = clientResponse.statusCode();

				serverHttpResponse.setStatusCode(httpStatusCode);

				_copyResponseHeaders(clientResponse, serverHttpResponse);

				if (httpStatusCode.isError()) {
					return serverHttpResponse.writeWith(
						clientResponse.bodyToFlux(DataBuffer.class));
				}

				HttpHeaders httpHeaders = serverHttpResponse.getHeaders();

				httpHeaders.setContentType(MediaType.TEXT_EVENT_STREAM);
				httpHeaders.setCacheControl("no-cache, no-store, no-transform");
				httpHeaders.remove(HttpHeaders.CONTENT_LENGTH);

				// Tells any intermediary, nginx in particular, to stop
				// collecting the response before passing it on.

				httpHeaders.set("X-Accel-Buffering", "no");

				Flux<DataBuffer> dataBuffers = clientResponse.bodyToFlux(
					DataBuffer.class);

				return serverHttpResponse.writeAndFlushWith(
					dataBuffers.map(Flux::just));
			}
		).onErrorResume(
			throwable -> _badGateway(serverHttpResponse, throwable)
		);
	}

	private Mono<Void> _badGateway(
		ServerHttpResponse serverHttpResponse, Throwable throwable) {

		_log.error(
			"Unable to reach the sidecar at {}: {}", _sidecar.getBaseUrl(),
			throwable.getMessage());

		if (serverHttpResponse.isCommitted()) {
			return Mono.error(throwable);
		}

		serverHttpResponse.setStatusCode(HttpStatus.BAD_GATEWAY);

		HttpHeaders httpHeaders = serverHttpResponse.getHeaders();

		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		return serverHttpResponse.writeWith(
			Mono.just(
				serverHttpResponse.bufferFactory(
				).wrap(
					"{\"message\":\"Sidecar unavailable\"}".getBytes()
				)));
	}

	private void _copyRequestHeaders(
		ServerHttpRequest serverHttpRequest, HttpHeaders target) {

		HttpHeaders source = serverHttpRequest.getHeaders();

		source.forEach(
			(name, values) -> {
				String lowerCaseName = name.toLowerCase(Locale.ROOT);

				if (!_SKIPPED_REQUEST_HEADERS.contains(lowerCaseName)) {
					target.addAll(name, values);
				}
			});
	}

	private void _copyResponseHeaders(
		ClientResponse clientResponse, ServerHttpResponse serverHttpResponse) {

		HttpHeaders target = serverHttpResponse.getHeaders();

		HttpHeaders source = clientResponse.headers().asHttpHeaders();

		source.forEach(
			(name, values) -> {
				String lowerCaseName = name.toLowerCase(Locale.ROOT);

				if (_SKIPPED_RESPONSE_HEADERS.contains(lowerCaseName) ||
					lowerCaseName.startsWith("access-control-")) {

					return;
				}

				target.addAll(name, values);
			});
	}

	private boolean _isBodyless(HttpStatusCode httpStatusCode) {
		int value = httpStatusCode.value();

		if ((value == 204) || (value == 205) || (value == 304)) {
			return true;
		}

		return false;
	}

	private URI _targetURI(ServerHttpRequest serverHttpRequest) {
		URI uri = serverHttpRequest.getURI();

		StringBuilder sb = new StringBuilder();

		sb.append(_sidecar.getBaseUrl());
		sb.append(serverHttpRequest.getPath().value());

		String rawQuery = uri.getRawQuery();

		if ((rawQuery != null) && !rawQuery.isEmpty()) {
			sb.append('?');
			sb.append(rawQuery);
		}

		return URI.create(sb.toString());
	}

	private static final Set<HttpMethod> _CARRIES_BODY = Set.of(
		HttpMethod.PATCH, HttpMethod.POST, HttpMethod.PUT);

	private static final Logger _log = LoggerFactory.getLogger(
		SidecarProxyController.class);

	// The broker never presents the caller credentials to the sidecar. Hop by
	// hop headers are dropped because they describe this connection only.

	private static final Set<String> _SKIPPED_REQUEST_HEADERS = Set.of(
		"accept-encoding", "authorization", "connection", "cookie", "expect",
		"host", "keep-alive", "origin", "proxy-authorization", "referer", "te",
		"trailer", "transfer-encoding", "upgrade");

	private static final Set<String> _SKIPPED_RESPONSE_HEADERS = Set.of(
		"connection", "content-encoding", "keep-alive", "proxy-authenticate",
		"te", "trailer", "transfer-encoding", "upgrade", "vary");

	private final BrokerProperties.Sidecar _sidecar;
	private final WebClient _sidecarWebClient;

}
