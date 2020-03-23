package io.gr1d.portal.subscriptions.service;

import io.gr1d.portal.subscriptions.response.RealmResponse;
import io.gr1d.spring.keycloak.Keycloak;
import io.gr1d.spring.keycloak.model.Realm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KeycloakRealmService {

    private final Keycloak keycloak;

    @Autowired
    public KeycloakRealmService(final Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    public List<RealmResponse> listRealms() {
        return keycloak.realms().stream()
                .filter(Realm::getEnabled)
                .map(RealmResponse::new)
                .collect(Collectors.toList());
    }

}
