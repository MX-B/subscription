package io.gr1d.integrations.whitelabel.model.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class V1TenantListResponse {

    private String uuid;
    private String realm;
    private String email;
    private String name;
    private String url;

    private LocalDateTime createdAt;
    private LocalDateTime removedAt;

}
