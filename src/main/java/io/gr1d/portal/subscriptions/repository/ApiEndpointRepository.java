package io.gr1d.portal.subscriptions.repository;

import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.model.ApiEndpoint;
import io.gr1d.portal.subscriptions.model.MethodType;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ApiEndpointRepository extends CrudRepository<ApiEndpoint, Long> {

	Optional<ApiEndpoint> findByApiAndPathAndMethodType(Api api, String path, MethodType methodType);

}
