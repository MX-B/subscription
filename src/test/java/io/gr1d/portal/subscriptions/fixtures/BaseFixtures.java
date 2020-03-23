package io.gr1d.portal.subscriptions.fixtures;

import br.com.six2six.fixturefactory.function.AtomicFunction;

import java.util.UUID;

public class BaseFixtures {

    public static class RandomUuid implements AtomicFunction {
        private final String prefix;
        private final String suffix;

        public RandomUuid() {
            prefix = suffix = "";
        }

        public RandomUuid(final String prefix) {
            this.prefix = prefix;
            suffix = "";
        }

        public RandomUuid(final String prefix, final String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override
        @SuppressWarnings("unchecked")
        public String generateValue() {
            return prefix + UUID.randomUUID().toString() + suffix;
        }
    }

}
