package io.gr1d.portal.subscriptions.service;

import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.model.Gr1dPage;
import io.gr1d.portal.subscriptions.api.recipients.Recipient;
import io.gr1d.portal.subscriptions.exception.ProviderNotFoundException;
import io.gr1d.portal.subscriptions.model.Provider;
import io.gr1d.portal.subscriptions.model.ProviderType;
import io.gr1d.portal.subscriptions.repository.ApiRepository;
import io.gr1d.portal.subscriptions.repository.ProviderRepository;
import io.gr1d.portal.subscriptions.request.ProviderRequest;
import io.gr1d.portal.subscriptions.response.ProviderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final ApiRepository apiRepository;
    private final RecipientService recipientService;

    @Autowired
    public ProviderService(final ProviderRepository providerRepository, final ApiRepository apiRepository, final RecipientService recipientService) {
        this.providerRepository = providerRepository;
        this.apiRepository = apiRepository;
        this.recipientService = recipientService;
    }

    public Gr1dPage<ProviderResponse> list(final Specification<Provider> spec, final Pageable pageable) {
        final Page<Provider> page = providerRepository.findAll(spec, pageable);
        final List<ProviderResponse> list = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return PageResult.ofPage(page, list);
    }

    @Transactional(rollbackFor = Throwable.class)
    public Provider save(final ProviderRequest request) {
        final Provider provider = new Provider();

        provider.setName(request.getName());
        provider.setWalletId(request.getWalletId());
        provider.setEmail(request.getEmail());
        provider.setPhone(request.getPhone());
        provider.setType(ProviderType.valueOf(request.getType()));

        return providerRepository.save(provider);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void update(final String uuid, final ProviderRequest request) throws ProviderNotFoundException {
        final Provider provider = findNotRemoved(uuid);

        provider.setName(request.getName());
        provider.setWalletId(request.getWalletId());
        provider.setEmail(request.getEmail());
        provider.setPhone(request.getPhone());
        provider.setType(ProviderType.valueOf(request.getType()));

        providerRepository.save(provider);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void remove(final String uuid) throws ProviderNotFoundException {
        final Provider provider = findNotRemoved(uuid);
        provider.setRemovedAt(LocalDateTime.now());
        apiRepository.removeApiByProvider(provider, LocalDateTime.now());
        providerRepository.save(provider);
    }

    public Provider findNotRemoved(final String uuid) throws ProviderNotFoundException {
        return providerRepository.findByUuidAndRemovedAtIsNull(uuid)
                .orElseThrow(ProviderNotFoundException::new);
    }

    public ProviderResponse find(final String uuid) throws ProviderNotFoundException {
        return providerRepository.findByUuid(uuid)
                .map(this::mapToResponse)
                .orElseThrow(ProviderNotFoundException::new);
    }

    public ProviderResponse mapToResponse(final Provider provider) {
        final Recipient recipient = ofNullable(provider.getWalletId())
                .map(recipientService::getRecipientData)
                .orElse(null);
        return new ProviderResponse(provider, recipient);
    }

    public String findByEmail(String email) {
        return providerRepository.findUUIDByEmail(email);
    }
}
