package io.gr1d.portal.subscriptions.controller.external.v1;

import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.portal.subscriptions.exception.TagNotFoundException;
import io.gr1d.portal.subscriptions.model.Tag;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@RequestMapping(path = "/v1/tag")
public class V1TagController extends BaseController {

    private final TagService tagService;

    public V1TagController(final TagService tagService) {
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
    @ApiOperation(nickname = "listTags", value = "List Tags", notes = "Returns a list with all registered Tags", tags = "V1 Tag")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public PageResult<Tag> list(final TagSpec specification, final Gr1dPageable page) {
        log.info("Listing Tags {}", page);
        return tagService.list(specification, page.toPageable());
    }

    @ApiOperation(nickname = "listTags", value = "List Tags", notes = "Returns a list with all registered Tags", tags = "V1 Tag")
    @RequestMapping(path = "/all", method = GET, produces = JSON)
    public List<Tag> listAll() {
        log.info("Listing all Tags");
        return tagService.listAll();
    }

    @ApiOperation(nickname = "getTagDetails", value = "Tag details", notes = "Returns information about a single Tag with given slug", tags = "V1 Tag")
    @RequestMapping(path = "/by-slug/{slug}", method = GET, produces = JSON)
    public Tag get(@PathVariable final String slug) throws TagNotFoundException {
        log.info("Requesting Tag by slug {}", slug);
        return tagService.findBySlug(slug);
    }

}
