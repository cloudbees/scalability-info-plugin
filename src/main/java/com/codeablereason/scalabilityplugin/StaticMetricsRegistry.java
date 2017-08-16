package com.codeablereason.scalabilityplugin;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.SlidingWindowReservoir;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.metrics.api.MetricProvider;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
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
@Extension
public class StaticMetricsRegistry extends MetricProvider {

    Meter runCompletedRate = new Meter();
    Histogram recentRunTime = new Histogram(new SlidingWindowReservoir(3));
    Meter flowNodeCreationMeter = new Meter();
    MetricSet metrics;

    /** NodeCounter requires {@link GraphListener} to be an extension point (workflow-api plugin 2.16)
     *  and also {@link CpsFlowExecution} to iterate over the extension point and fire notifiers (workflow-cps plugin 2.33)
     *
     *  Otherwise we shouldn't even try to register this metric because it will not work
     */
    public static boolean canCountFlownodes() {
        // If GraphListener isn't an extension point then this won't work
        if (ExtensionPoint.class.getClass().isAssignableFrom(GraphListener.class)) {
            /*Method m = hudson.util.ReflectionUtils.findMethod(CpsFlowExecution.class, "getListenersToRun", null);
            if (m != null) {
                return true;
            }*/
            return true;
        }
        return false;
    }


    @Extension
    public static class NodeCounter implements GraphListener {
        StaticMetricsRegistry registry = null;

        @Override
        public void onNewHead(FlowNode flowNode) {
            if (registry == null) {
                registry = Jenkins.getActiveInstance().getExtensionList(StaticMetricsRegistry.class).get(0);
            }
            if (registry.flowNodeCreationMeter != null) {
                // Protects against early-initialization NPEs
                registry.flowNodeCreationMeter.mark();
            }
        }
    }

    @Extension
    public static class RunCompletionListener extends RunListener<Run> {
        StaticMetricsRegistry registry = null;

        @Override
        public void onCompleted(Run run, @Nonnull TaskListener listener) {
            if (registry == null) {
                registry = Jenkins.getActiveInstance().getExtensionList(StaticMetricsRegistry.class).get(0);
            }
            if (run != null && registry.runCompletedRate != null && registry.recentRunTime != null) {
                // Protects against early-initialization NPEs
                registry.runCompletedRate.mark();
                registry.recentRunTime.update(run.getDuration());
            }
        }
    }

    @NonNull
    @Override
    public MetricSet getMetricSet() {
        if (StaticMetricsRegistry.canCountFlownodes()) {  // Don't register it if useless
            metrics = metrics(metric(name("jenkins", "scalemetrics", "runCompletedRate"), runCompletedRate),
                    metric(name("jenkins", "scalemetrics", "recentRunTime"), recentRunTime),
                    metric(name("jenkins", "scalemetrics", "flownodeCreation"), flowNodeCreationMeter)
            );
        } else { // No flownode counters
            metrics = metrics(metric(name("jenkins", "scalemetrics", "runCompletedRate"), runCompletedRate),
                    metric(name("jenkins", "scalemetrics", "recentRunTime"), recentRunTime)
            );
        }

        return metrics;
    }
}
