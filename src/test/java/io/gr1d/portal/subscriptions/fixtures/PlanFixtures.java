package io.gr1d.portal.subscriptions.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.portal.subscriptions.request.PlanEndpointRangeRequest;
import io.gr1d.portal.subscriptions.request.PlanEndpointRequest;
import io.gr1d.portal.subscriptions.request.PlanRequest;

import java.util.HashMap;
import java.util.Map;

public class PlanFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(PlanEndpointRequest.class).addTemplate("valid_unlimited", new Rule() {
            {
                add("endpoint", "/test/endpoint/");
                add("name", firstName());

                Map<String, String> metadata = new HashMap<>();
                metadata.put("hit_value", "1500");
                add("metadata", metadata);
            }
        });
        Fixture.of(PlanEndpointRequest.class).addTemplate("valid_unlimited_2", new Rule() {
            {
                add("endpoint", "/test/another_endpoint/");
                add("name", firstName());
                add("methodType", "POST");

                Map<String, String> metadata = new HashMap<>();
                metadata.put("hit_value", "1900");
                add("metadata", metadata);
            }
        });

        Fixture.of(PlanEndpointRequest.class).addTemplate("invalid_hit_valid_unlimited", new Rule() {
            {
                add("endpoint", "/test/endpoint/");
                add("name", firstName());
                add("methodType", "GET");

                Map<String, String> metadata = new HashMap<>();
                metadata.put("hit_value", String.valueOf(-10L));
                add("metadata", metadata);
            }
        });

        Fixture.of(PlanEndpointRequest.class).addTemplate("hitValueTooLarge_unlimited", new Rule() {
            {
                add("endpoint", "/test/endpoint/");
                add("name", firstName());
                add("methodType", "PUT");

                Map<String, String> metadata = new HashMap<>();
                metadata.put("hit_value", "123456789123456789123");
                add("metadata", metadata);
            }
        });

        Fixture.of(PlanEndpointRequest.class).addTemplate("valid_default", new Rule() {
            {
                add("endpoint", "/test/endpoint/");
                add("name", firstName());
                add("methodType", "DELETE");

                Map<String, String> metadata = new HashMap<>();
                metadata.put("hits", "1500");
                metadata.put("hit_value", "1500");
                add("metadata", metadata);
            }
        });

        Fixture.of(PlanEndpointRequest.class).addTemplate("valid_free", new Rule() {
            {
                add("endpoint", "/test/endpoint/");
                add("name", firstName());

                Map<String, String> metadata = new HashMap<>();
                metadata.put("limit", "100");
                add("metadata", metadata);
            }
        });

        Fixture.of(PlanEndpointRequest.class).addTemplate("invalidLimit_free", new Rule() {
            {
                add("endpoint", "/test/endpoint/");
                add("name", firstName());
                add("methodType", "PUT");

                Map<String, String> metadata = new HashMap<>();
                metadata.put("limit", String.valueOf(-10L));
                add("metadata", metadata);
            }
        });

        Fixture.of(PlanEndpointRequest.class).addTemplate("invalid_name").inherits("valid_unlimited", new Rule() {
            {
                add("name", null);
            }
        });

        Fixture.of(PlanEndpointRangeRequest.class).addTemplate("valid_unlimited_range_1", new Rule() {
            {
                add("initRange", 0L);
                add("finalRange", 19L);
                add("value", 500L);
            }
        });

        Fixture.of(PlanEndpointRangeRequest.class).addTemplate("valid_unlimited_range_2", new Rule() {
            {
                add("initRange", 20L);
                add("finalRange", 29L);
                add("value", 300L);
            }
        });

        Fixture.of(PlanEndpointRangeRequest.class).addTemplate("valid_unlimited_range_3", new Rule() {
            {
                add("initRange", 30L);
                add("value", 200L);
            }
        });

        Fixture.of(PlanEndpointRequest.class).addTemplate("valid_unlimited_range", new Rule() {
            {
                add("endpoint", "/test/endpoint/");
                add("name", firstName());
                add("ranges", has(3).of(PlanEndpointRangeRequest.class,
                        "valid_unlimited_range_1", "valid_unlimited_range_2", "valid_unlimited_range_3"));
            }
        });

        Fixture.of(PlanRequest.class).addTemplate("valid_unlimited", new Rule() {
            {
                add("modality", "UNLIMITED");
                add("description", "Lorem ipsum");
                add("name", "BASIC");
                add("planEndpoints", has(1).of(PlanEndpointRequest.class, "valid_unlimited"));
            }
        });

        Fixture.of(PlanRequest.class).addTemplate("invalid_hit_valid_unlimited", new Rule() {
            {
                add("modality", "UNLIMITED");
                add("description", "Lorem ipsum");
                add("name", "BASIC");
                add("planEndpoints", has(1).of(PlanEndpointRequest.class, "invalid_hit_valid_unlimited"));
            }
        });
        Fixture.of(PlanRequest.class).addTemplate("hitValueTooLarge_unlimited", new Rule() {
            {
                add("modality", "UNLIMITED");
                add("description", "Lorem ipsum");
                add("name", "BASIC");
                add("planEndpoints", has(1).of(PlanEndpointRequest.class, "hitValueTooLarge_unlimited"));
            }
        });
        Fixture.of(PlanRequest.class).addTemplate("invalidLimit_free", new Rule() {
            {
                add("modality", "FREE");
                add("description", "Lorem ipsum");
                add("name", "FREE");
                add("planEndpoints", has(1).of(PlanEndpointRequest.class, "invalidLimit_free"));
            }
        });
        Fixture.of(PlanRequest.class).addTemplate("valid_unlimited_range", new Rule() {
            {
                add("modality", "UNLIMITED_RANGE");
                add("description", "Lorem ipsum");
                add("name", "ENTERPRISE");
                add("planEndpoints", has(1).of(PlanEndpointRequest.class, "valid_unlimited_range"));
            }
        });
    }

}
