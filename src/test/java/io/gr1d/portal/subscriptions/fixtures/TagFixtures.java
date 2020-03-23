package io.gr1d.portal.subscriptions.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.portal.subscriptions.model.Tag;
import io.gr1d.portal.subscriptions.request.TagRequest;

public class TagFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(Tag.class).addTemplate("valid", new Rule() {
            {
                add("name", name());
                add("description", name());
                add("slug", new BaseFixtures.RandomUuid());
            }
        });

        Fixture.of(TagRequest.class).addTemplate("valid", new Rule() {
            {
                add("name", name());
                add("description", name());
                add("slug", new BaseFixtures.RandomUuid());
            }
        });

        Fixture.of(TagRequest.class).addTemplate("invalidName").inherits("valid", new Rule() {
            {
                add("name", "");
            }
        });

        Fixture.of(TagRequest.class).addTemplate("invalidSlug").inherits("valid", new Rule() {
            {
                add("slug", "um slug com espaços");
            }
        });
    }

}
