package com.codeablereason.scalabilityplugin;

import com.codahale.metrics.Metric;
import hudson.tasks.LogRotator;
import jenkins.metrics.api.Metrics;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Sam Van Oort
 */
public class PluginTest {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void testPlugin() {
        Map<String, Metric> metrics = Metrics.metricRegistry().getMetrics();
        Assert.assertTrue(metrics.containsKey(name("jenkins", "scalemetrics", "recentRunTime")));
    }

    @Test
    public void testMetersIncrement() throws Exception {
        StaticMetricsRegistry registry = jenkinsRule.jenkins.getExtensionList(StaticMetricsRegistry.class).get(0);
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "TrivialJob");
        job.setBuildDiscarder(new LogRotator(-1, 20, -1, 40));
        job.setDefinition(new CpsFlowDefinition("echo 'I did something'", false));
        jenkinsRule.buildAndAssertSuccess(job);
        jenkinsRule.buildAndAssertSuccess(job);
        jenkinsRule.buildAndAssertSuccess(job);

        Assert.assertEquals(3, registry.recentRunTime.getCount());
        double val = registry.recentRunTime.getSnapshot().getMedian();
        Assert.assertTrue("Median run time should be over zero but instead wasn't: ", val > 0.0);

        Assert.assertEquals(3, registry.runCompletedRate.getCount());
        val = registry.runCompletedRate.getOneMinuteRate();
        Assert.assertTrue("Run completed 1 minute rate should be over zero but instead wasn't: ", val > 0.0);
        val = registry.runCompletedRate.getMeanRate();
        Assert.assertTrue("Run completed mean rate should be over zero but instead wasn't: ", val > 0.0);

        Assert.assertTrue("Should have nonzero flownode creation count", registry.flowNodeCreationMeter.getCount() > 0);
        Assert.assertTrue("Should have nonzero flownode creation 1 minute rate", registry.flowNodeCreationMeter.getOneMinuteRate() > 0.0);
    }
}
