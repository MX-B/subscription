package io.gr1d.portal.subscriptions.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.portal.subscriptions.fixtures.BaseFixtures.RandomUuid;
import io.gr1d.portal.subscriptions.model.Tenant;
import io.gr1d.portal.subscriptions.request.TenantCreateRequest;

import java.time.LocalDateTime;
import java.util.UUID;

public class TenantFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(Tenant.class).addTemplate("valid", new Rule() {
            {
                add("name", name());
                add("realm", "innovation-cloud");
                add("uuid", new RandomUuid("TEN-"));
                add("walletId", new RandomUuid());
                add("createdAt", LocalDateTime.now());
            }
        });

        Fixture.of(TenantCreateRequest.class).addTemplate("valid", new Rule() {
            {
                add("name", name());
                add("logo", "C1hNUCBEYXRhWE1QPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQ");
                add("walletId", UUID.randomUUID().toString());
                add("email", "example@test.net");
                add("phone", "+5511999999999");
                add("supportEmail", "support@test.net");
                add("realm", firstName());
                add("url", name());
            }
        });

        Fixture.of(TenantCreateRequest.class).addTemplate("invalidName").inherits("valid", new Rule() {
            {
                add("name", "");
            }
        });

        Fixture.of(TenantCreateRequest.class).addTemplate("invalidWallet").inherits("valid", new Rule() {
            {
                add("walletId", "");
            }
        });

        Fixture.of(TenantCreateRequest.class).addTemplate("invalidEmail").inherits("valid", new Rule() {
            {
                add("email", "example.net");
            }
        });

        Fixture.of(TenantCreateRequest.class).addTemplate("invalidSupportEmail").inherits("valid", new Rule() {
            {
                add("supportEmail", "example.net");
            }
        });

        Fixture.of(TenantCreateRequest.class).addTemplate("invalidPhone").inherits("valid", new Rule() {
            {
                add("phone", "12345");
            }
        });

        Fixture.of(TenantCreateRequest.class).addTemplate("invalidRealm").inherits("valid", new Rule() {
            {
                add("realm", null);
            }
        });

        Fixture.of(TenantCreateRequest.class).addTemplate("invalidLogo").inherits("valid", new Rule() {
            {
                add("logo", "");
            }
        });
    }

    public static class TenantRule extends Rule {
        public TenantRule(Tenant tenant) {
            add("tenant", tenant.getUuid());
        }
    }
}
