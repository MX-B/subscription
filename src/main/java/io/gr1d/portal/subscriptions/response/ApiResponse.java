package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.api.bridge.ApiBridge;
import io.gr1d.portal.subscriptions.model.Api;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Getter
public class ApiResponse extends ApiListResponse {

    private final ApiBridge apiBridge;
    private final PlanResponse plan;

    private final List<TagResponse> tags;

    public ApiResponse(final Api api, final ApiBridge apiBridge) {
        super(api);

        this.plan = ofNullable(api.getPlan()).map(PlanResponse::new).orElse(null);
        this.apiBridge = apiBridge;

        this.tags = api.getTags().stream()
                .map(TagResponse::new)
                .collect(Collectors.toList());
    }

}
