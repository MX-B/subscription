package io.gr1d.integrations.whitelabel;

import io.gr1d.core.model.CreatedResponse;
import io.gr1d.integrations.whitelabel.model.request.V1TenantRequest;
import io.gr1d.integrations.whitelabel.model.response.V1TenantResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "whitelabelApi", url = "${gr1d.integration.whitelabelService}", configuration = KeycloakFeignConfiguration.class)
public interface V1WhitelabelApi {

//    @GetMapping("/v1/tenant?sort={sort}&page={page}&page_size={page_size}")
//    Gr1dPage<V1TenantListResponse> list(@PathVariable("sort") String sort,
//                                        @PathVariable("page") Integer page,
//                                        @PathVariable("page_size") Integer pageSize);
//
//    @GetMapping("/v1/tenant?sort={sort}&page={page}&page_size={page_size}&complete=true")
//    Gr1dPage<V1TenantResponse> listComplete(@PathVariable("sort") String sort,
//                                            @PathVariable("page") Integer page,
//                                            @PathVariable("page_size") Integer pageSize);

    @PostMapping("/v1/tenant")
    CreatedResponse create(@RequestBody V1TenantRequest request);

    @GetMapping("/v1/tenant/by-realm/{realm}")
    V1TenantResponse get(@PathVariable("realm") String realm);

    @PutMapping("/v1/tenant/by-realm/{realm}")
    void update(@PathVariable("realm") String uuid,
                @RequestBody V1TenantRequest request);

//    @PostMapping("/v1/tenant/{uuid}/asset")
//    void uploadAsset(@PathVariable("uuid") String uuid,
//                     @RequestBody V1TenantAssetRequest request);
//
//    @DeleteMapping("/v1/tenant/{uuid}/asset/{namespace}/{name}")
//    void deleteAsset(@PathVariable("uuid") String uuid,
//                     @PathVariable("namespace") String namespace,
//                     @PathVariable("name") String name);
//
//    @PostMapping("/v1/tenant/{uuid}/field")
//    void setField(@PathVariable("uuid") String uuid,
//                  @RequestBody V1TenantFieldRequest request);
//
//    @DeleteMapping("/v1/tenant/{uuid}/field/{namespace}/{name}")
//    void unsetField(@PathVariable("uuid") String uuid,
//                    @PathVariable("namespace") String namespace,
//                    @PathVariable("name") String name);
//
//    @GetMapping("/v1/tenant/{realm}/fields/{namespace}")
//    Map<String, Object> getFieldsByNamespace(@PathVariable("realm") String realm,
//                                             @PathVariable("namespace") String namespace);

}
