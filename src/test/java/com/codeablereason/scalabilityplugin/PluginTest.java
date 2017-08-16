package com.codeablereason.scalabilityplugin;

import com.codahale.metrics.Metric;
import jenkins.metrics.api.Metrics;
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
    JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void testPlugin() {
        Map<String, Metric> metrics = Metrics.metricRegistry().getMetrics();
        Assert.assertTrue(metrics.containsKey(name("jenkins", "scalemetrics", "flownodeCreation")));
    }
}
