package io.gr1d.portal.subscriptions.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.model.ApiGateway;
import io.gr1d.portal.subscriptions.model.Gateway;
import io.gr1d.portal.subscriptions.request.ApiGatewayCreateRequest;
import io.gr1d.portal.subscriptions.request.ApiGatewayUpdateRequest;

public class ApiGatewayFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(ApiGateway.class).addTemplate("valid", new Rule() {
            {
                add("externalId", new BaseFixtures.RandomUuid());
                add("productionExternalId", new BaseFixtures.RandomUuid());
            }
        });

        Fixture.of(ApiGatewayCreateRequest.class).addTemplate("valid", new Rule() {
            {
                add("externalId", new BaseFixtures.RandomUuid());
                add("productionExternalId", new BaseFixtures.RandomUuid());
            }
        });

        Fixture.of(ApiGatewayCreateRequest.class).addTemplate("invalidExternalId", new Rule() {
            {
                add("externalId", "");
                add("productionExternalId", new BaseFixtures.RandomUuid());
            }
        });

        Fixture.of(ApiGatewayUpdateRequest.class).addTemplate("valid", new Rule() {
            {
                add("externalId", new BaseFixtures.RandomUuid());
                add("productionExternalId", new BaseFixtures.RandomUuid());
            }
        });
    }

    public static class ApiGatewayRequestRule extends Rule {
        public ApiGatewayRequestRule(final Api api, final Gateway gateway) {
            add("apiUuid", api.getUuid());
            add("gatewayUuid", gateway.getUuid());
        }
    }

    public static class ApiGatewayRule extends Rule {
        public ApiGatewayRule(final Api api, final Gateway gateway) {
            add("api", api);
            add("gateway", gateway);
        }
    }

}
