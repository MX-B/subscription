package io.gr1d.portal.subscriptions.request;

import io.gr1d.core.datasource.validation.Unique;
import io.gr1d.portal.subscriptions.model.Api;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Getter@Setter
@ToString(callSuper = true)
public class ApiCreateRequest extends ApiRequest {

    @NotEmpty
    @Size(max = 64)
    @Unique(message = "{io.gr1d.subscriptions.validation.Unique.apiMessage}",
            property = "externalId", entity = Api.class)
    private String externalId;

}
