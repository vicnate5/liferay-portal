package com.liferay.aihub.broker;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

/**
 * Wires the outbound HTTP client and the cross origin policy.
 */
@Configuration
public class BrokerWebConfig {

	/**
	 * Runs ahead of {@link AuthWebFilter} so a preflight request is answered
	 * before authentication is considered.
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public CorsWebFilter corsWebFilter(BrokerProperties brokerProperties) {
		CorsConfiguration corsConfiguration = new CorsConfiguration();

		BrokerProperties.Cors cors = brokerProperties.getCors();

		corsConfiguration.setAllowCredentials(true);
		corsConfiguration.setAllowedHeaders(
			List.of(
				HttpHeaders.ACCEPT, HttpHeaders.AUTHORIZATION,
				HttpHeaders.CACHE_CONTROL, HttpHeaders.CONTENT_TYPE,
				"Last-Event-ID"));
		corsConfiguration.setAllowedMethods(
			List.of("GET", "POST", "OPTIONS"));
		corsConfiguration.setAllowedOrigins(List.of(cors.getAllowedOrigin()));
		corsConfiguration.setExposedHeaders(
			List.of(HttpHeaders.CONTENT_TYPE));
		corsConfiguration.setMaxAge(1800L);

		UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource =
			new UrlBasedCorsConfigurationSource();

		urlBasedCorsConfigurationSource.registerCorsConfiguration(
			"/**", corsConfiguration);

		return new CorsWebFilter(urlBasedCorsConfigurationSource);
	}

	/**
	 * Client used to reach DXP for token validation. A short response timeout
	 * is appropriate here because the call is a plain request and response.
	 */
	@Bean("dxpWebClient")
	public WebClient dxpWebClient(BrokerProperties brokerProperties) {
		BrokerProperties.Auth auth = brokerProperties.getAuth();

		return WebClient.builder(
		).baseUrl(
			auth.getDxpBaseUrl()
		).clientConnector(
			new ReactorClientHttpConnector(HttpClient.create().compress(false))
		).build();
	}

	/**
	 * Client used to reach the sidecar.
	 *
	 * <p>
	 * Compression is disabled on purpose. A compressing codec would sit
	 * between the sidecar and the browser and coalesce server sent event
	 * frames, which is exactly the failure the contract forbids. No response
	 * timeout is set either, because a stream is expected to stay open.
	 * </p>
	 */
	@Bean("sidecarWebClient")
	public WebClient sidecarWebClient(BrokerProperties brokerProperties) {
		BrokerProperties.Sidecar sidecar = brokerProperties.getSidecar();

		return WebClient.builder(
		).baseUrl(
			sidecar.getBaseUrl()
		).clientConnector(
			new ReactorClientHttpConnector(
				HttpClient.create(
				).compress(
					false
				).keepAlive(
					true
				))
		).build();
	}

}
