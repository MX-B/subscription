package io.gr1d.integrations.whitelabel;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;
import feign.slf4j.Slf4jLogger;
import io.gr1d.core.exception.Gr1dConstraintException;
import io.gr1d.core.response.Gr1dError;
import io.gr1d.spring.keycloak.interceptor.KeycloakAuthInterceptor;
import io.gr1d.spring.keycloak.interceptor.KeycloakErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

import static java.util.Optional.ofNullable;

@Slf4j
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
    public ErrorDecoder errorDecoder(final ObjectMapper objectMapper) {
        return new WhitelabelErrorDecoder(keycloakAuthInterceptor, objectMapper);
    }

    private class WhitelabelErrorDecoder extends KeycloakErrorDecoder {

        private final ObjectMapper objectMapper;

        private WhitelabelErrorDecoder(final KeycloakAuthInterceptor interceptor, final ObjectMapper objectMapper) {
            super(interceptor);
            this.objectMapper = objectMapper;
        }

        @Override
        public Exception decode(final String methodKey, final Response response) {
            if (response.status() == 422 && response.body() != null)  {
                try {
                    final Gr1dError[] errors = objectMapper.readValue(response.body().asReader(), Gr1dError[].class);
                    if (errors.length > 0) {
                        return new Gr1dConstraintException(errors[0].getMeta(), errors[0].getMessage(), false);
                    }
                } catch (IOException e) {
                    log.error("Error while trying to serialize microservice message", e);
                }
            }
            return super.decode(methodKey, response);
        }
    }

    @Bean
    public Logger logger() {
        return new Slf4jLogger(getClass());
    }
}
