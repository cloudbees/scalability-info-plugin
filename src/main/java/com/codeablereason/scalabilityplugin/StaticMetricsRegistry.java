package com.codeablereason.scalabilityplugin;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.SlidingWindowReservoir;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import jenkins.metrics.api.MetricProvider;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Metrics that don't dynamically get created and destroyed, primarily around builds and flows
 * @author Sam Van Oort
 */
@Restricted(NoExternalUse.class)
@Extension
@SuppressFBWarnings(value = "IS2_INCONSISTENT_SYNC", justification = "Metrics synchronize internally")
public class StaticMetricsRegistry extends MetricProvider {

    /** Rate at which runs have completed (as separate from the scheduling rate) */
    Meter runCompletedRate = new Meter();

    /** Time to complete builds finished in the last minute */
    Histogram recentRunTime = new Histogram(new SlidingTimeWindowReservoir(1, TimeUnit.MINUTES));

    /** Rate at which we are generating FlowNodes... if using plugin versions supporting this */
    Meter flowNodeCreationMeter = new Meter();

    Histogram flowNodeRunTime = new Histogram(new SlidingTimeWindowReservoir(1, TimeUnit.MINUTES));

    MetricSet metrics;

    /** NodeCounter requires {@link GraphListener} to be an extension point (workflow-api plugin 2.16)
     *  and also {@link CpsFlowExecution} to iterate over the extension point and fire notifiers (workflow-cps plugin 2.33)
     *
     *  Otherwise we shouldn't even try to register this metric because it will not work.
     *  Note: tested manually, because we don't want to mandate a more recent core version (2.7.3 needed)
     */
    public static boolean canCountFlownodes() {
        // If GraphListener isn't an extension point then this won't work (missing workflow-api support)
        if (ExtensionPoint.class.isAssignableFrom(GraphListener.class)) {
            // Below signals workflow-cps support we need
            Method m = hudson.util.ReflectionUtils.findMethod(CpsFlowExecution.class, "getListenersToRun", null);
            if (m != null) {
                return true;
            }
            return true;
        }
        return false;
    }


    @Extension
    public static class NodeCounter implements GraphListener {
        StaticMetricsRegistry registry = null;

        // Tested manually due to issues with bumping the base dependencies

        @Override
        public void onNewHead(FlowNode flowNode) {
            synchronized (this) {
                if (registry == null) {
                    registry = Jenkins.getActiveInstance().getExtensionList(StaticMetricsRegistry.class).get(0);
                }
            }
            registry.flowNodeCreationMeter.mark();
            List<FlowNode> n = flowNode.getParents();
            if (n.size() == 1) { // Can't compute if the first node and ignore parallels with multiple parents for now
                FlowNode parent = n.get(0);
                long runTime = TimingAction.getStartTime(flowNode)-TimingAction.getStartTime(parent);
                registry.flowNodeRunTime.update(runTime);
            }
        }
    }

    @Extension
    public static class RunCompletionListener extends RunListener<Run> {
        StaticMetricsRegistry registry = null;

        @Override
        public void onFinalized(Run run) {
            if (run == null) {
                return;
            }
            synchronized (this) {
                if (registry == null) {
                    registry = Jenkins.getActiveInstance().getExtensionList(StaticMetricsRegistry.class).get(0);
                }
            }
            registry.runCompletedRate.mark();
            registry.recentRunTime.update(run.getDuration());
        }
    }

    @NonNull
    @Override
    public MetricSet getMetricSet() {
        if (StaticMetricsRegistry.canCountFlownodes()) {  // Don't register it if useless
            metrics = metrics(metric(name("jenkins", "scalemetrics", "runCompletedRate"), runCompletedRate),
                    metric(name("jenkins", "scalemetrics", "recentRunTime"), recentRunTime),
                    metric(name("jenkins", "scalemetrics", "flownodeCreation"), flowNodeCreationMeter),
                            metric(name("jenkins", "scalemetrics", "flowNodeTime"), flowNodeRunTime)
            );
        } else { // No flownode counters
            metrics = metrics(metric(name("jenkins", "scalemetrics", "runCompletedRate"), runCompletedRate),
                    metric(name("jenkins", "scalemetrics", "recentRunTime"), recentRunTime)
            );
        }

        return metrics;
    }
}
