package com.codeablereason.scalabilityplugin;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import hudson.Extension;
import jenkins.metrics.api.Metrics;
import jenkins.plugin.randomjobbuilder.GeneratorController;
import jenkins.plugin.randomjobbuilder.GeneratorControllerListener;
import jenkins.plugin.randomjobbuilder.LoadGenerator;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reports metrics on load generators
 * @author Sam Van Oort
 */
@Extension
public class LoadGeneratorRegistry {

    /** Maps generator ID to metrics */
    static ConcurrentHashMap<String,Metric> generatorIdToMetrics = new ConcurrentHashMap();

    /** Return how many tasks are currently being run */
    static class LoadGeneratorTaskCountGauge implements Gauge<Integer> {
        private LoadGenerator gen;

        public String getGeneratorShortName() {
            return gen.getShortName();
        }

        @Override
        public Integer getValue() {
            return GeneratorController.getInstance().getQueuedAndRunningCount(this.gen);
        }

        LoadGeneratorTaskCountGauge(@Nonnull LoadGenerator gen) {
            this.gen = gen;
        }
    }

    @Extension
    @Restricted(NoExternalUse.class)
    public static class ControllerListenerImpl extends GeneratorControllerListener {

        @Override
        public void onGeneratorAdded(LoadGenerator loadGenerator) {
            LoadGeneratorTaskCountGauge gauge = new LoadGeneratorTaskCountGauge(loadGenerator);
            generatorIdToMetrics.put(loadGenerator.getGeneratorId(), gauge);
            Metrics.metricRegistry().register(
                    MetricRegistry.name("jenkins", "loadgenerators",gauge.getGeneratorShortName(),"currentTaskCount"),
                    gauge);
        }

        @Override
        public void onGeneratorRemoved(LoadGenerator loadGenerator) {
            // No-op, we can't exactly kill the generator
        }

        @Override
        public void onGeneratorStarted(LoadGenerator loadGenerator) {
            if (!generatorIdToMetrics.containsKey(loadGenerator.getGeneratorId())) {
                this.onGeneratorAdded(loadGenerator);
            }
            // No-op, currently -- counts just do not increae
        }

        @Override
        public void onGeneratorStopped(LoadGenerator loadGenerator) {
            if (!generatorIdToMetrics.containsKey(loadGenerator.getGeneratorId())) {
                this.onGeneratorAdded(loadGenerator);
            }
            // No-op, currently --  -- counts just do not increase
        }

        @Override
        public void onGeneratorReconfigured(LoadGenerator old, LoadGenerator newGen) {
            if (!generatorIdToMetrics.containsKey(newGen.getGeneratorId())) {
                this.onGeneratorAdded(newGen);
            }
        }
    }
}
