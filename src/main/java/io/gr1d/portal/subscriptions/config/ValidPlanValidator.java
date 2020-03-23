package io.gr1d.portal.subscriptions.config;

import io.gr1d.portal.subscriptions.exception.InvalidPropertyException;
import io.gr1d.portal.subscriptions.model.PlanModality;
import io.gr1d.portal.subscriptions.request.PlanEndpointRangeRequest;
import io.gr1d.portal.subscriptions.request.PlanEndpointRequest;
import io.gr1d.portal.subscriptions.request.PlanRequest;
import io.gr1d.portal.subscriptions.service.PlanModalityStrategy;
import io.gr1d.portal.subscriptions.service.plan.PlanModalityStrategyResolver;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ValidPlanValidator implements ConstraintValidator<ValidPlan, PlanRequest> {

    private final PlanModalityStrategyResolver strategyResolver;

    @Autowired
    public ValidPlanValidator(final PlanModalityStrategyResolver strategyResolver) {
        this.strategyResolver = strategyResolver;
    }

    private boolean invalidModality(final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("{io.gr1d.subscriptions.validation.PlanModality.notFound}")
                .addPropertyNode("modality")
                .addConstraintViolation();
        return false;
    }

    private boolean requiredValue(final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("{io.gr1d.subscriptions.validation.value.required}")
                .addPropertyNode("value")
                .addConstraintViolation();
        return false;
    }


    @Override
    public boolean isValid(final PlanRequest planRequest, final ConstraintValidatorContext context) {
        final PlanModality modality = PlanModality.valueOf(planRequest.getModality());

        if (modality == null) {
            return invalidModality(context);
        }

        if (planRequest.getPlanEndpoints() == null) {
            planRequest.setPlanEndpoints(new ArrayList<>());
        }

        if ((modality.equals(PlanModality.DEFAULT) || modality.equals(PlanModality.UNLIMITED_RANGE)) && planRequest.getValue() == null) {
            return requiredValue(context);
        }

        final PlanModalityStrategy strategy = strategyResolver.resolve(modality);

        if (strategy == null) {
            return invalidModality(context);
        }

        if (modality.equals(PlanModality.UNLIMITED_RANGE)) {

            return validateUnlimitedRange(planRequest, context);

        } else {
            final AtomicInteger planEndpointNode = new AtomicInteger();
            try {

                planRequest.getPlanEndpoints().forEach(planEndpointRequest -> {
                    strategy.validateAndProcessMetadata(planEndpointRequest.getMetadata());
                    planEndpointNode.set(planEndpointNode.intValue()+1);
                });
            } catch (final InvalidPropertyException e) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(e.getMessage())
                        .addPropertyNode("planEndpoints[" + planEndpointNode.intValue() + "].metadata")
                        .addPropertyNode(null).inIterable().atKey(e.getProperty())
                        .addConstraintViolation();
                return false;
            }
        }

        return true;
    }

    private boolean validateUnlimitedRange(PlanRequest planRequest, ConstraintValidatorContext context) {
        final AtomicInteger planEndpointNode = new AtomicInteger();
        for (PlanEndpointRequest planEndpointRequest:planRequest.getPlanEndpoints()) {

            if (planEndpointRequest.getRanges() == null || planEndpointRequest.getRanges().isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("{io.gr1d.subscriptions.validation.ranges.empty}")
                        .addPropertyNode("planEndpoints[" + planEndpointNode.intValue() + "].ranges")
                        .addConstraintViolation();
                return false;
            }

            final List<PlanEndpointRangeRequest> ranges = planEndpointRequest.getRanges();

            if (ranges.get(0).getInitRange() != 0) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("{io.gr1d.subscriptions.validation.ranges.initialMustBeZero}")
                        .addPropertyNode("planEndpoints[" + planEndpointNode.intValue() + "].ranges[0].initRange")
                        .addConstraintViolation();
                return false;
            }

            for (int i = 0; i < ranges.size(); i++) {

                if (ranges.get(i).getFinalRange() != null && ranges.get(i).getInitRange() > ranges.get(i).getFinalRange()) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("{io.gr1d.subscriptions.validation.ranges.finalMustGreaterInitValue}")
                            .addPropertyNode("planEndpoints[" + planEndpointNode.intValue() + "].ranges["+ i +"].finalRange")
                            .addConstraintViolation();
                    return false;
                }

                if (ranges.size() > i+1) {

                    if (ranges.get(i).getFinalRange() == null || (ranges.get(i).getFinalRange() + 1 != ranges.get(i+1).getInitRange())) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("{io.gr1d.subscriptions.validation.ranges.initialNextMustGreaterFinalValue}")
                                .addPropertyNode("planEndpoints[" + planEndpointNode.intValue() + "].ranges["+ (i+1) +"].initRange")
                                .addConstraintViolation();
                        return false;
                    }
                } else {
                    if (ranges.get(i).getFinalRange() != null) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("{io.gr1d.subscriptions.validation.ranges.finishFinalMustEmpty}")
                                .addPropertyNode("planEndpoints[" + planEndpointNode.intValue() + "].ranges["+ i +"].finalRange")
                                .addConstraintViolation();
                        return false;
                    }
                }
            }

            planEndpointNode.incrementAndGet();
        }
        return true;
    }

}
