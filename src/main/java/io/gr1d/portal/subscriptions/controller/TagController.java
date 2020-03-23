package io.gr1d.portal.subscriptions.controller;

import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.portal.subscriptions.exception.TagNotFoundException;
import io.gr1d.portal.subscriptions.model.Tag;
import io.gr1d.portal.subscriptions.request.TagRequest;
import io.gr1d.portal.subscriptions.service.TagService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.domain.Like;
import net.kaczmarzyk.spring.data.jpa.domain.NotNull;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@Slf4j
@RestController
@RequestMapping(path = "/tag")
public class TagController extends BaseController {

    private final TagService tagService;

    public TagController(final TagService tagService) {
        this.tagService = tagService;
    }

    @And({
            @Spec(path = "name", params = "name", spec = Like.class),
            @Spec(path = "slug", params = "slug", spec = Equal.class),
            @Spec(path = "removedAt", params = "removed", spec = NotNull.class, constVal = "false")
    })
    private interface TagSpec extends Specification<Tag> {
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "slug", dataType = "string", paramType = "query")
    })
    @ApiOperation(nickname = "listTags", value = "List Tags", notes = "Returns a list with all registered Tags", tags = "Tag")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public PageResult<Tag> list(final TagSpec specification, final Gr1dPageable page) {
        log.info("Listing Tags {}", page);
        return tagService.list(specification, page.toPageable());
    }

    @ApiOperation(nickname = "createTag", value = "Create Tag", notes = "Creates a new Tag", tags = "Tag")
    @RequestMapping(path = "", method = POST, consumes = JSON, produces = JSON)
    public ResponseEntity<CreatedResponse> create(@Valid @RequestBody final TagRequest request) {
        log.info("Creating Tag with request {}", request);
        final Tag tag = tagService.save(request);
        final URI uri = URI.create(String.format("/tag/%s", tag.getUuid()));
        return ResponseEntity.created(uri).body(new CreatedResponse(tag.getUuid()));
    }

    @ApiOperation(nickname = "updateTag", value = "Update Tag", notes = "Updates an existing Tag", tags = "Tag")
    @RequestMapping(path = "/{uuid}", method = PUT, consumes = JSON)
    public void update(@PathVariable final String uuid,
                       @Valid @RequestBody final TagRequest request) throws TagNotFoundException {
        log.info("Updating Tag {} with request {}", uuid, request);
        tagService.update(uuid, request);
    }

    @ApiOperation(nickname = "removeTag", value = "Remove Tag", notes = "Removes an existing Tag", tags = "Tag")
    @RequestMapping(path = "/{uuid}", method = DELETE)
    public void remove(@PathVariable final String uuid) throws TagNotFoundException {
        log.info("Removing Tag {}", uuid);
        tagService.remove(uuid);
    }

    @ApiOperation(nickname = "getTagDetails", value = "Tag details", notes = "Returns information about a single Tag with given uuid", tags = "Tag")
    @RequestMapping(path = "/{uuid}", method = GET, produces = JSON)
    public Tag get(@PathVariable final String uuid) throws TagNotFoundException {
        log.info("Requesting Tag {}", uuid);
        return tagService.find(uuid);
    }

}
