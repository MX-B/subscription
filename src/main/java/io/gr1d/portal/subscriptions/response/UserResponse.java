package io.gr1d.portal.subscriptions.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {

    private String userId;
    private String realm;

}
