package io.gr1d.portal.subscriptions.service;

import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.exception.Gr1dConstraintException;
import io.gr1d.portal.subscriptions.exception.TagNotFoundException;
import io.gr1d.portal.subscriptions.model.Tag;
import io.gr1d.portal.subscriptions.repository.TagRepository;
import io.gr1d.portal.subscriptions.request.TagRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TagService {

    private final TagRepository tagRepository;

    private final Specification<Tag> notRemoved = (root, criteriaQuery, criteriaBuilder)
            -> root.get("removedAt").isNull();

    @Autowired
    public TagService(final TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public PageResult<Tag> list(final Specification<Tag> spec, final Pageable pageable) {
        return PageResult.ofPage(tagRepository.findAll(spec, pageable));
    }

    public List<Tag> listAll() {
        return tagRepository.findAll(notRemoved);
    }

    public Tag find(final String uuid) throws TagNotFoundException {
        return tagRepository.findByUuid(uuid)
                .orElseThrow(TagNotFoundException::new);
    }

    public Tag findBySlug(final String uuid) throws TagNotFoundException {
        return tagRepository.findBySlugAndRemovedAtIsNull(uuid)
                .orElseThrow(TagNotFoundException::new);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized Tag save(final TagRequest request) {
        final Tag tag = new Tag();
        checkSlugUnique(tag, request.getSlug());

        tag.setName(request.getName());
        tag.setSlug(request.getSlug());
        return tagRepository.save(tag);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized void update(final String uuid, final TagRequest request) throws TagNotFoundException {
        final Tag tag = findOrThrow(uuid);
        checkSlugUnique(tag, request.getSlug());
        tag.setName(request.getName());
        tag.setSlug(request.getSlug());
        tagRepository.save(tag);
    }

    private void checkSlugUnique(final Tag tag, final String slug) {
        final Optional<Tag> returnedTag = tagRepository.findBySlugAndRemovedAtIsNull(slug);
        if (returnedTag.isPresent() && !returnedTag.get().equals(tag)) {
            throw new Gr1dConstraintException("slug", "io.gr1d.subscriptions.tagSlugUnique");
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void remove(final String uuid) throws TagNotFoundException {
        final Tag tag = findOrThrow(uuid);

        log.info("Removing Tag with uuid: {}", uuid);
        tag.setRemovedAt(LocalDateTime.now());
        tagRepository.save(tag);
        tagRepository.deleteAssociations(tag.getId());
    }

    private Tag findOrThrow(final String uuid) throws TagNotFoundException {
        return tagRepository.findByUuidAndRemovedAtIsNull(uuid)
                .orElseThrow(TagNotFoundException::new);
    }
}
