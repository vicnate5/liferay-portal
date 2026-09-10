package com.liferay.aihub.broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point for the AI Hub Workspace broker.
 *
 * <p>
 * The broker sits between the chat client extension hosted by DXP on port 8080
 * and the agent sidecar on port 8090. It authenticates every call against DXP
 * and proxies the contract API through to the sidecar without buffering.
 * </p>
 */
@EnableConfigurationProperties(BrokerProperties.class)
@SpringBootApplication
public class BrokerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BrokerApplication.class, args);
	}

}
