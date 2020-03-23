package io.gr1d.portal.subscriptions.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.portal.subscriptions.model.Category;
import io.gr1d.portal.subscriptions.request.CategoryRequest;

public class CategoryFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(Category.class).addTemplate("valid", new Rule() {
            {
                add("name", name());
                add("description", name());
                add("slug", new BaseFixtures.RandomUuid());
            }
        });

        Fixture.of(CategoryRequest.class).addTemplate("valid", new Rule() {
            {
                add("name", name());
                add("description", name());
                add("slug", new BaseFixtures.RandomUuid());
            }
        });

        Fixture.of(CategoryRequest.class).addTemplate("invalidName").inherits("valid", new Rule() {
            {
                add("name", "");
            }
        });

        Fixture.of(CategoryRequest.class).addTemplate("invalidSlug").inherits("valid", new Rule() {
            {
                add("slug", "um slug com espaços");
            }
        });
    }

}
