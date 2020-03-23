package io.gr1d.portal.subscriptions.controller.external.v1;

import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.portal.subscriptions.exception.CategoryNotFoundException;
import io.gr1d.portal.subscriptions.model.Category;
import io.gr1d.portal.subscriptions.service.CategoryService;
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
@RequestMapping(path = "/v1/category")
public class V1CategoryController extends BaseController {

    private final CategoryService categoryService;

    public V1CategoryController(final CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @And({
            @Spec(path = "name", params = "name", spec = Like.class),
            @Spec(path = "slug", params = "slug", spec = Equal.class),
            @Spec(path = "removedAt", params = "removed", spec = NotNull.class, constVal = "false")
    })
    private interface CategorySpec extends Specification<Category> {
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "slug", dataType = "string", paramType = "query")
    })
    @ApiOperation(nickname = "listCategories", value = "List Categories", notes = "Returns a list with all registered Categories", tags = "V1 Category")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public PageResult<Category> list(final CategorySpec specification, final Gr1dPageable page) {
        log.info("Listing Categories {}", page);
        return categoryService.list(specification, page.toPageable());
    }

    @ApiOperation(nickname = "listCategories", value = "List Categories", notes = "Returns a list with all registered Categories", tags = "V1 Category")
    @RequestMapping(path = "/all", method = GET, produces = JSON)
    public List<Category> listAll() {
        log.info("Listing all Categories");
        return categoryService.listAll();
    }

    @ApiOperation(nickname = "getCategoryDetails", value = "Category details", notes = "Returns information about a single Category with given slug", tags = "V1 Category")
    @RequestMapping(path = "/by-slug/{slug}", method = GET, produces = JSON)
    public Category get(@PathVariable final String slug) throws CategoryNotFoundException {
        log.info("Requesting Category by slug {}", slug);
        return categoryService.findBySlug(slug);
    }

}
