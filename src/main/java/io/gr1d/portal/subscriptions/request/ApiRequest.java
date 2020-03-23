package io.gr1d.portal.subscriptions.request;

import io.gr1d.core.validation.Value;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.List;

@ToString
@Getter@Setter
public class ApiRequest {

    @NotEmpty
    private String name;

    @NotEmpty
    private String provider;

    @NotEmpty
    @Value(values = { "AUTO", "MANUAL" })
    @ApiModelProperty("Can be `AUTO` or `MANUAL`, when `AUTO` a Plan must be provided. Default `MANUAL`")
    private String splitMode = "MANUAL";

    @NotEmpty
    @Length(max = 60)
    @Pattern(regexp = "^[a-z0-9\\-]*$")
    private String slug;

    private List<String> tags;
    private String category;

    private PlanRequest plan;

    private String currency;

    @AssertTrue(message = "{javax.validation.constraints.NotEmpty.message}")
    private boolean isPlan() {
        return !"AUTO".equals(splitMode) || (plan != null && "AUTO".equals(splitMode));
    }

}
