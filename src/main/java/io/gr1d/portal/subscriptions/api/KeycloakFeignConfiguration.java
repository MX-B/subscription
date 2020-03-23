package io.gr1d.portal.subscriptions.api;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import feign.slf4j.Slf4jLogger;
import io.gr1d.spring.keycloak.interceptor.KeycloakAuthInterceptor;
import io.gr1d.spring.keycloak.interceptor.KeycloakErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class KeycloakFeignConfiguration {
    private final KeycloakAuthInterceptor keycloakAuthInterceptor;

    public KeycloakFeignConfiguration(@Value("${keycloak.auth-server-url:${gr1d.keycloak.serviceAccount.url}}") final String baseUrl,
                                      @Value("${gr1d.keycloak.serviceAccount.realm}") final String realm,
                                      @Value("${gr1d.keycloak.serviceAccount.clientId}") final String clientId,
                                      @Value("${gr1d.keycloak.serviceAccount.clientSecret}") final String clientSecret) {

        keycloakAuthInterceptor = new KeycloakAuthInterceptor(baseUrl, realm, clientId, clientSecret);
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return keycloakAuthInterceptor;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new KeycloakErrorDecoder(keycloakAuthInterceptor);
    }

    @Bean
    public Logger logger() {
        return new Slf4jLogger(getClass());
    }
}
