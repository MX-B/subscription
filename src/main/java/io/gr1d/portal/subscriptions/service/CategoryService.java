package io.gr1d.portal.subscriptions.service;

import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.exception.Gr1dConstraintException;
import io.gr1d.portal.subscriptions.exception.CategoryNotFoundException;
import io.gr1d.portal.subscriptions.model.Category;
import io.gr1d.portal.subscriptions.repository.CategoryRepository;
import io.gr1d.portal.subscriptions.request.CategoryRequest;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private final Specification<Category> notRemoved = (root, criteriaQuery, criteriaBuilder)
            -> root.get("removedAt").isNull();

    @Autowired
    public CategoryService(final CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public PageResult<Category> list(final Specification<Category> spec, final Pageable pageable) {
        return PageResult.ofPage(categoryRepository.findAll(spec, pageable));
    }

    public List<Category> listAll() {
        return categoryRepository.findAll(notRemoved);
    }

    public Category find(final String uuid) throws CategoryNotFoundException {
        return categoryRepository.findByUuid(uuid)
                .orElseThrow(CategoryNotFoundException::new);
    }

    public Category findBySlug(final String uuid) throws CategoryNotFoundException {
        return categoryRepository.findBySlugAndRemovedAtIsNull(uuid)
                .orElseThrow(CategoryNotFoundException::new);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized Category save(final CategoryRequest request) {
        final Category category = new Category();
        checkSlugUnique(category, request.getSlug());
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        return categoryRepository.save(category);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized void update(final String uuid, final CategoryRequest request) throws CategoryNotFoundException {
        final Category category = findOrThrow(uuid);
        checkSlugUnique(category, request.getSlug());
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        categoryRepository.save(category);
    }

    private void checkSlugUnique(final Category category, final String slug) {
        final Optional<Category> returnedCategory = categoryRepository.findBySlugAndRemovedAtIsNull(slug);
        if (returnedCategory.isPresent() && !returnedCategory.get().equals(category)) {
            throw new Gr1dConstraintException("slug", "io.gr1d.subscriptions.categorySlugUnique");
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void remove(final String uuid) throws CategoryNotFoundException {
        final Category category = findOrThrow(uuid);

        log.info("Removing Category with uuid: {}", uuid);
        category.setRemovedAt(LocalDateTime.now());
        categoryRepository.save(category);
        categoryRepository.deleteAssociations(category.getId());
    }

    private Category findOrThrow(final String uuid) throws CategoryNotFoundException {
        return categoryRepository.findByUuidAndRemovedAtIsNull(uuid)
                .orElseThrow(CategoryNotFoundException::new);
    }
}
