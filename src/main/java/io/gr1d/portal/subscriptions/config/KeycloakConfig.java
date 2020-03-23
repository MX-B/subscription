package io.gr1d.portal.subscriptions.config;

import io.gr1d.spring.keycloak.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {
	
	@Bean
	public Keycloak keycloak(@Value("${keycloak.auth-server-url}") final String baseUrl,
			@Value("${gr1d.keycloak.serviceAccount.realm}") final String realm,
			@Value("${gr1d.keycloak.serviceAccount.clientId}") final String clientId,
			@Value("${gr1d.keycloak.serviceAccount.clientSecret}") final String clientSecret) {
		
		return new Keycloak(baseUrl, realm, clientId, clientSecret);
	}
	
}
