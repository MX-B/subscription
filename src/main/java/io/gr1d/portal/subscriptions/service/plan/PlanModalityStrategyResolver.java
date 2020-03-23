package io.gr1d.portal.subscriptions.service.plan;

import io.gr1d.core.strategy.StrategyResolver;
import io.gr1d.portal.subscriptions.model.PlanModality;
import io.gr1d.portal.subscriptions.service.PlanModalityStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlanModalityStrategyResolver {

    private final StrategyResolver strategyResolver;

    @Autowired
    public PlanModalityStrategyResolver(final StrategyResolver strategyResolver) {
        this.strategyResolver = strategyResolver;
    }

    public PlanModalityStrategy resolve(final PlanModality planModality) {
        return strategyResolver.resolve(PlanModalityStrategy.class, planModality.getCode());
    }

}
