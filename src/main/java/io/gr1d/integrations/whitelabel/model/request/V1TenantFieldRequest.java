package io.gr1d.integrations.whitelabel.model.request;

import lombok.*;

@Builder
@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
public class V1TenantFieldRequest {

    public static final String PUBLIC = "PUBLIC";
    public static final String PRIVATE = "PRIVATE";

    private String namespace;
    private String name;
    private Object value;
    private String scope;

}
