package io.gr1d.portal.subscriptions.repository;

import io.gr1d.core.datasource.repository.BaseModelRepository;
import io.gr1d.portal.subscriptions.model.Category;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends BaseModelRepository<Category> {

    Optional<Category> findBySlugAndRemovedAtIsNull(String slug);

    @Modifying
    @Query(value = "UPDATE api SET category_id = NULL WHERE category_id = :categoryId", nativeQuery = true)
    void deleteAssociations(@Param("categoryId") Long categoryId);

}
