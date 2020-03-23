package io.gr1d.portal.subscriptions.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.model.SplitMode;
import io.gr1d.portal.subscriptions.request.ApiCreateRequest;
import io.gr1d.portal.subscriptions.request.PlanRequest;

import java.util.UUID;

public class ApiFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(Api.class).addTemplate("valid", new Rule() {
            {
                add("name", name());
                add("slug", new BaseFixtures.RandomUuid());
                add("externalId", new BaseFixtures.RandomUuid());
                add("splitMode", SplitMode.MANUAL);
            }
        });

        Fixture.of(ApiCreateRequest.class).addTemplate("valid", new Rule() {
            {
                add("name", name());
                add("slug", new BaseFixtures.RandomUuid());
                add("provider", "PRV-" + UUID.randomUUID().toString());
                add("externalId", new BaseFixtures.RandomUuid());
                add("splitMode", SplitMode.MANUAL.getName());
            }
        });

        Fixture.of(ApiCreateRequest.class).addTemplate("invalidName").inherits("valid",  new Rule() {
            {
                add("name", "");
            }
        });

        Fixture.of(ApiCreateRequest.class).addTemplate("splitAuto").inherits("valid",  new Rule() {
            {
                add("splitMode", "AUTO");
                add("plan", one(PlanRequest.class, "valid_unlimited"));
            }
        });
    }

    public static class ApiRule extends Rule {
        public ApiRule(final Api api) {
            add("apiUuid", api.getUuid());
        }
    }
}
