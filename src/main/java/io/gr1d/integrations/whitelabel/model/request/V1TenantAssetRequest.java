package io.gr1d.integrations.whitelabel.model.request;

import lombok.*;

@Builder
@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
public class V1TenantAssetRequest {

    private String namespace;
    private String name;
    private String extension;
    private String content;

}
