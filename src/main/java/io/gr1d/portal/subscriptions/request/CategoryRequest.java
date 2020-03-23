package io.gr1d.portal.subscriptions.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@ToString
@Getter@Setter
public class CategoryRequest {

    @NotEmpty
    @Length(max = 60)
    private String name;

    @NotEmpty
    @Length(max = 60)
    @Pattern(regexp = "^[a-z0-9\\-]*$")
    private String slug;

    @Length(max = 160)
    private String description;

}
