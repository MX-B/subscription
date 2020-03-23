package io.gr1d.portal.subscriptions.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.portal.subscriptions.fixtures.BaseFixtures.RandomUuid;
import io.gr1d.portal.subscriptions.model.Provider;
import io.gr1d.portal.subscriptions.model.ProviderType;
import io.gr1d.portal.subscriptions.request.ProviderRequest;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProviderFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(Provider.class).addTemplate("valid", new Rule() {
            {
                add("name", name());
                add("uuid", new RandomUuid("PRV-"));
                add("phone", "+5511999999999");
                add("email", "provider@test.net");
                add("type", ProviderType.PAID);
                add("walletId", UUID.randomUUID().toString());
                add("createdAt", LocalDateTime.now());
            }
        });

        Fixture.of(Provider.class).addTemplate("validFree", new Rule() {
            {
                add("name", name());
                add("uuid", new RandomUuid("PRV-"));
                add("phone", "+5511999999999");
                add("email", "provider@test.net");
                add("type", ProviderType.FREE);
                add("createdAt", LocalDateTime.now());
            }
        });

        Fixture.of(ProviderRequest.class).addTemplate("valid", new Rule() {
            {
                add("name", name());
                add("email", "example@test.net");
                add("phone", "+5511999999999");
                add("type", "FREE");
            }
        });

        Fixture.of(ProviderRequest.class).addTemplate("validPaid", new Rule() {
            {
                add("name", name());
                add("walletId", UUID.randomUUID().toString());
                add("email", "example@test.net");
                add("phone", "+5511999999999");
                add("type", "PAID");
            }
        });

        Fixture.of(ProviderRequest.class).addTemplate("invalidName", new Rule() {
            {
                add("name", "");
                add("walletId", UUID.randomUUID().toString());
                add("email", "example@test.net");
                add("phone", "+5511999999999");
                add("type", "PAID");
            }
        });

        Fixture.of(ProviderRequest.class).addTemplate("invalidWallet", new Rule() {
            {
                add("name", name());
                add("walletId", "");
                add("email", "example@test.net");
                add("phone", "+5511999999999");
                add("type", "PAID");
            }
        });

        Fixture.of(ProviderRequest.class).addTemplate("invalidEmail", new Rule() {
            {
                add("name", name());
                add("walletId", UUID.randomUUID().toString());
                add("email", "example.net");
                add("phone", "+5511999999999");
                add("type", "PAID");
            }
        });

        Fixture.of(ProviderRequest.class).addTemplate("invalidPhone", new Rule() {
            {
                add("name", name());
                add("walletId", UUID.randomUUID().toString());
                add("email", "example@test.net");
                add("phone", "12345");
                add("type", "PAID");
            }
        });
    }

    public static class ProviderRule extends Rule {
        public ProviderRule(final Provider provider) {
            add("provider", provider.getUuid());
        }
        public ProviderRule(final String providerUuid) {
            add("provider", providerUuid);
        }
    }
}
