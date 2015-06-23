package com.palantir.gerrit.gerritci;

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.webui.GwtPlugin;
import com.google.gerrit.extensions.webui.WebUiPlugin;
import com.google.gerrit.httpd.plugins.HttpPluginModule;
import com.palantir.gerrit.gerritci.servlets.JobsServlet;

/**
 * This class binds custom servlets and filters to the Gerrit HTTP server.
 */
public class HttpModule extends HttpPluginModule {

    @Override
    protected void configureServlets() {
        // Gerrit-CI settings page
        DynamicSet.bind(binder(), WebUiPlugin.class).toInstance(new GwtPlugin("settings"));

        // Jenkins jobs REST API
        serve("/jobs").with(JobsServlet.class);
    }
}
