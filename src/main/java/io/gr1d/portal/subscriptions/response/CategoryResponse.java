package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.model.Category;
import lombok.Getter;

@Getter
public class CategoryResponse {

    private final String uuid;
    private final String slug;
    private final String name;
    private final String description;

    public CategoryResponse(final Category category) {
        this.uuid = category.getUuid();
        this.slug = category.getSlug();
        this.name = category.getName();
        this.description = category.getDescription();
    }

}
