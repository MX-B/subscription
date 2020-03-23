package io.gr1d.portal.subscriptions.controller;

import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.portal.subscriptions.exception.CategoryNotFoundException;
import io.gr1d.portal.subscriptions.model.Category;
import io.gr1d.portal.subscriptions.request.CategoryRequest;
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
@RequestMapping(path = "/category")
public class CategoryController extends BaseController {

    private final CategoryService categoryService;

    public CategoryController(final CategoryService categoryService) {
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
    @ApiOperation(nickname = "listCategorys", value = "List Categorys", notes = "Returns a list with all registered Categorys", tags = "Category")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public PageResult<Category> list(final CategorySpec specification, final Gr1dPageable page) {
        log.info("Listing Categorys {}", page);
        return categoryService.list(specification, page.toPageable());
    }

    @ApiOperation(nickname = "createCategory", value = "Create Category", notes = "Creates a new Category", tags = "Category")
    @RequestMapping(path = "", method = POST, consumes = JSON, produces = JSON)
    public ResponseEntity<CreatedResponse> create(@Valid @RequestBody final CategoryRequest request) {
        log.info("Creating Category with request {}", request);
        final Category category = categoryService.save(request);
        final URI uri = URI.create(String.format("/category/%s", category.getUuid()));
        return ResponseEntity.created(uri).body(new CreatedResponse(category.getUuid()));
    }

    @ApiOperation(nickname = "updateCategory", value = "Update Category", notes = "Updates an existing Category", tags = "Category")
    @RequestMapping(path = "/{uuid}", method = PUT, consumes = JSON)
    public void update(@PathVariable final String uuid,
                       @Valid @RequestBody final CategoryRequest request) throws CategoryNotFoundException {
        log.info("Updating Category {} with request {}", uuid, request);
        categoryService.update(uuid, request);
    }

    @ApiOperation(nickname = "removeCategory", value = "Remove Category", notes = "Removes an existing Category", tags = "Category")
    @RequestMapping(path = "/{uuid}", method = DELETE)
    public void remove(@PathVariable final String uuid) throws CategoryNotFoundException {
        log.info("Removing Category {}", uuid);
        categoryService.remove(uuid);
    }

    @ApiOperation(nickname = "getCategoryDetails", value = "Category details", notes = "Returns information about a single Category with given uuid", tags = "Category")
    @RequestMapping(path = "/{uuid}", method = GET, produces = JSON)
    public Category get(@PathVariable final String uuid) throws CategoryNotFoundException {
        log.info("Requesting Category {}", uuid);
        return categoryService.find(uuid);
    }

}
