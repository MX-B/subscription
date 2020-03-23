package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.model.Tag;
import lombok.Getter;

@Getter
public class TagResponse {

    private final String uuid;
    private final String slug;
    private final String name;
    private final String description;

    public TagResponse(final Tag tag) {
        this.uuid = tag.getUuid();
        this.slug = tag.getSlug();
        this.name = tag.getName();
        this.description = tag.getDescription();
    }

}
