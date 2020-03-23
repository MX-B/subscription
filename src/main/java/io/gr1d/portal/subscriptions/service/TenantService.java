package io.gr1d.portal.subscriptions.service;

import feign.FeignException;
import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.model.Gr1dPage;
import io.gr1d.integrations.whitelabel.V1WhitelabelApi;
import io.gr1d.integrations.whitelabel.model.request.V1TenantAssetRequest;
import io.gr1d.integrations.whitelabel.model.request.V1TenantFieldRequest;
import io.gr1d.integrations.whitelabel.model.request.V1TenantRequest;
import io.gr1d.integrations.whitelabel.model.response.V1TenantResponse;
import io.gr1d.portal.subscriptions.api.recipients.Recipient;
import io.gr1d.portal.subscriptions.exception.TenantNotFoundException;
import io.gr1d.portal.subscriptions.model.Tenant;
import io.gr1d.portal.subscriptions.repository.TenantRepository;
import io.gr1d.portal.subscriptions.request.TenantCreateRequest;
import io.gr1d.portal.subscriptions.request.TenantRequest;
import io.gr1d.portal.subscriptions.response.TenantResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ApplicationContext applicationContext;
    private final V1WhitelabelApi whitelabelApi;
    private final RecipientService recipientService;

    @Autowired
    public TenantService(final TenantRepository tenantRepository,
                         final ApplicationContext applicationContext,
                         final V1WhitelabelApi whitelabelApi,
                         final RecipientService recipientService) {
        this.tenantRepository = tenantRepository;
        this.applicationContext = applicationContext;
        this.whitelabelApi = whitelabelApi;
        this.recipientService = recipientService;
    }

    @Transactional(rollbackFor = Throwable.class)
    public Tenant save(final TenantCreateRequest request) {
        V1TenantResponse byRealm = null;
        try {
            byRealm = whitelabelApi.get(request.getRealm());
        } catch (FeignException e) {
            log.warn("There's already a Whitelabel with this realm {}", request.getRealm());
        }

        final V1TenantFieldRequest supportEmail = field("support_email", request.getSupportEmail());
        final V1TenantFieldRequest phone = field("phone", request.getPhone());
        final List<V1TenantAssetRequest> assets = upload(request);
        final V1TenantRequest tenantRequest = V1TenantRequest.builder()
                .email(request.getEmail())
                .name(request.getName())
                .url(request.getUrl())
                .realm(request.getRealm())
                .assets(assets)
                .fields(Arrays.asList(supportEmail, phone))
                .build();

        if (byRealm != null) {
            whitelabelApi.update(byRealm.getRealm(), tenantRequest);
        } else {
            whitelabelApi.create(tenantRequest);
        }

        final Tenant tenant = new Tenant();
        tenant.setWalletId(request.getWalletId());
        tenant.setName(request.getName());
        tenant.setRealm(request.getRealm());
        return tenantRepository.save(tenant);
    }

    private V1TenantFieldRequest field(final String name, final String value) {
        return V1TenantFieldRequest.builder()
                .namespace("payments")
                .name(name)
                .value(value)
                .build();
    }

    @Transactional(rollbackFor = Throwable.class)
    public Tenant update(final String uuid, final TenantRequest request) throws TenantNotFoundException {
        final Tenant tenant = findNotRemoved(uuid);
        final V1TenantFieldRequest supportEmail = field("support_email", request.getSupportEmail());
        final V1TenantFieldRequest phone = field("phone", request.getPhone());
        final List<V1TenantAssetRequest> assets = upload(request);

        whitelabelApi.update(tenant.getRealm(), V1TenantRequest.builder()
                .email(request.getEmail())
                .name(request.getName())
                .url(request.getUrl())
                .fields(Arrays.asList(supportEmail, phone))
                .assets(assets)
                .build());

        tenant.setName(request.getName());
        return tenantRepository.save(tenant);
    }

    private List<V1TenantAssetRequest> upload(final TenantRequest request) {
        if (StringUtils.isNotEmpty(request.getLogo())) {
            return Collections.singletonList(V1TenantAssetRequest.builder()
                    .namespace("email")
                    .name("logo_header")
                    .extension("png")
                    .content(request.getLogo())
                    .build());
        }
        return null;
    }

    Tenant findNotRemoved(final String uuid) throws TenantNotFoundException {
        return tenantRepository.findByUuidAndRemovedAtIsNull(uuid)
                .orElseThrow(TenantNotFoundException::new);
    }

    public TenantResponse find(final String uuid) throws TenantNotFoundException {
        return tenantRepository.findByUuid(uuid)
                .map(this::toResponse)
                .orElseThrow(TenantNotFoundException::new);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void remove(final String uuid) throws TenantNotFoundException {
        final Tenant tenant = findNotRemoved(uuid);
        tenant.setRemovedAt(LocalDateTime.now());
        tenantRepository.save(tenant);

        final ApiGatewayTenantService apiGatewayTenantService = applicationContext.getBean(ApiGatewayTenantService.class);
        apiGatewayTenantService.removeByTenant(tenant);
    }

    public TenantResponse findByRealm(final String realm) throws TenantNotFoundException {
        return tenantRepository.findByRealmAndRemovedAtIsNull(realm)
                .map(this::toResponse)
                .orElseThrow(TenantNotFoundException::new);
    }

    public Gr1dPage<TenantResponse> list(final Specification<Tenant> spec, final Pageable pageable) {
        final Page<Tenant> page = tenantRepository.findAll(spec, pageable);
        final List<TenantResponse> list = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResult.ofPage(page, list);
    }

    private TenantResponse toResponse(final Tenant tenant) {
        final V1TenantResponse response = whitelabelApi.get(tenant.getRealm());
        final Recipient recipient = recipientService.getRecipientData(tenant.getWalletId());
        return new TenantResponse(tenant, response, recipient);
    }
}
