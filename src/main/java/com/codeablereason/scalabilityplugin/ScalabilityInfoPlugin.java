package com.codeablereason.scalabilityplugin;

import hudson.Plugin;
import jenkins.model.Jenkins;

/**
 * Does all the registration magic we need
 * @author Sam Van Oort
 */
public class ScalabilityInfoPlugin extends Plugin {
    // Don't really DO anything

    StaticMetricsRegistry staticMetrics;

    @Override
    public void postInitialize() {
        staticMetrics = Jenkins.getActiveInstance().getExtensionList(StaticMetricsRegistry.class).get(0);
    }
}
