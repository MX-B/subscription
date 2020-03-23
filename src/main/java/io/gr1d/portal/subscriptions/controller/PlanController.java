package io.gr1d.portal.subscriptions.controller;

import io.gr1d.core.controller.BaseController;
import io.gr1d.portal.subscriptions.exception.PlanNotFoundException;
import io.gr1d.portal.subscriptions.response.PlanResponse;
import io.gr1d.portal.subscriptions.service.PlanService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@RequestMapping(path = "/plan")
public class PlanController extends BaseController {

    private final PlanService planService;

    @Autowired
    public PlanController(final PlanService planService) {
        this.planService = planService;
    }

    @ApiOperation(value = "Get Plan", notes = "Returns a Plan by its uuid", tags = "Plan")
    @RequestMapping(path = "/{uuid}", method = GET, produces = JSON)
    public PlanResponse getPlan(@PathVariable final String uuid) throws PlanNotFoundException {
        log.info("Requesting a Plan by uuid {}", uuid);
        return planService.find(uuid);
    }
}
