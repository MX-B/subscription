package io.gr1d.integrations.whitelabel.model.request;

import lombok.*;

import java.util.List;

@Builder
@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
public class V1TenantRequest {

    private String realm;
    private String url;
    private String name;
    private String email;

    private List<V1TenantFieldRequest> fields;
    private List<V1TenantAssetRequest> assets;

}
