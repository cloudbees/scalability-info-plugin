package com.codeablereason.scalabilityplugin;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.SlidingWindowReservoir;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.metrics.api.MetricProvider;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Metrics that don't dynamically get created and destroyed
 * @author Sam Van Oort
 */
@Restricted(NoExternalUse.class)
public class StaticMetricsRegistry extends MetricProvider {

    Meter flowNodeCreationMeter;
    Meter runCompletedRate;
    Histogram recentRunTime;
    MetricSet metrics;

    @Extension
    public static class NodeCounter implements GraphListener {
        StaticMetricsRegistry registry = null;

        @Override
        public void onNewHead(FlowNode flowNode) {
            if (registry == null) {
                registry = Jenkins.getActiveInstance().getExtensionList(StaticMetricsRegistry.class).get(0);
            }
            registry.flowNodeCreationMeter.mark();
        }
    }

    @Extension
    public static class RunCompletionListener extends RunListener {
        StaticMetricsRegistry registry = null;

        @Override
        public void onCompleted(Run run, @Nonnull TaskListener listener) {
            if (registry == null) {
                registry = Jenkins.getActiveInstance().getExtensionList(StaticMetricsRegistry.class).get(0);
            }
            registry.runCompletedRate.mark();
            if (run != null) {
                registry.recentRunTime.update(run.getDuration());
            }
        }
    }

    @NonNull
    @Override
    public MetricSet getMetricSet() {
        metrics = metrics(metric(name("jenkins", "scalemetrics", "flownodeCreation"), (flowNodeCreationMeter = new Meter())),
                  metric(name("jenkins", "scalemetrics", "flownodeCreation"), (runCompletedRate = new Meter())),
                metric(name("jenkins", "scalemetrics", "recentRunTime"), (recentRunTime = new Histogram(new SlidingWindowReservoir(3))))
        );

        return metrics;
    }
}
