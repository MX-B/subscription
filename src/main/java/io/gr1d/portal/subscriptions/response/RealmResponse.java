package io.gr1d.portal.subscriptions.response;

import io.gr1d.spring.keycloak.model.Realm;
import lombok.Data;

@Data
public class RealmResponse {

    private String id;
    private String name;

    public RealmResponse(final Realm realm) {
        this.id = realm.getRealm();
        this.name = realm.getDisplayName();
    }

}
