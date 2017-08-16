package com.codeablereason.scalabilityplugin;

import com.codahale.metrics.Gauge;
import jenkins.plugin.randomjobbuilder.GeneratorController;
import jenkins.plugin.randomjobbuilder.LoadGenerator;

import javax.annotation.Nonnull;

/**
 * Reports metrics on load generators
 * @author Sam Van Oort
 */
public class LoadGeneratorRegistry {
   /* static class LoadGeneratorGauge implements Gauge<LoadGenerator> {
        String generatorId;

        public LoadGeneratorGauge(@Nonnull String generatorId) {
            this.generatorId = generatorId;
        }

        @Override
        public LoadGenerator getValue() {
            return GeneratorController.getInstance().getRegisteredGeneratorbyId(generatorId);
        }
    }*/
}
