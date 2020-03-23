package io.gr1d.portal.subscriptions.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.portal.subscriptions.model.Gateway;
import io.gr1d.portal.subscriptions.request.GatewayCreateRequest;

public class GatewayFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(Gateway.class).addTemplate("valid", new Rule() {
            {
                add("name", name());
                add("externalId", new BaseFixtures.RandomUuid());
            }
        });

        Fixture.of(GatewayCreateRequest.class).addTemplate("valid", new Rule() {
            {
                add("name", name());
                add("externalId", new BaseFixtures.RandomUuid());
            }
        });

        Fixture.of(GatewayCreateRequest.class).addTemplate("invalidName").inherits("valid", new Rule() {
            {
                add("name", "");
            }
        });

        Fixture.of(GatewayCreateRequest.class).addTemplate("invalidExternalId").inherits("valid", new Rule() {
            {
                add("externalId", "");
            }
        });
    }

    public static class GatewayRule extends Rule {
        public GatewayRule(final Gateway gateway) {
            add("gatewayUuid", gateway.getUuid());
        }
    }

}
