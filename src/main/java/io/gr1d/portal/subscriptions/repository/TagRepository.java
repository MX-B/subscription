package io.gr1d.portal.subscriptions.repository;

import io.gr1d.core.datasource.repository.BaseModelRepository;
import io.gr1d.portal.subscriptions.model.Tag;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends BaseModelRepository<Tag> {

    Optional<Tag> findBySlugAndRemovedAtIsNull(String slug);

    @Modifying
    @Query(value = "DELETE FROM api_tag WHERE tag_id = :tagId", nativeQuery = true)
    void deleteAssociations(@Param("tagId") Long tagId);

}
