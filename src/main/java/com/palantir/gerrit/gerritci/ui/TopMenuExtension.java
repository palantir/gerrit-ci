package com.palantir.gerrit.gerritci.ui;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.webui.GerritTopMenu;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.inject.Inject;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.palantir.gerrit.gerritci.constants.JobType;
import com.palantir.gerrit.gerritci.models.JenkinsServerConfiguration;
import com.palantir.gerrit.gerritci.providers.JenkinsProvider;
import com.palantir.gerrit.gerritci.providers.VelocityProvider;

/**
 * This class extends the Gerrit top menu and allows us to add sections and entries based on current
 * user permissions, project selection, and more.
 */
public class TopMenuExtension implements TopMenu {

    private static final Logger logger = LoggerFactory.getLogger(TopMenuExtension.class);

    // Entries for the top menu. Sub-items for each entry are added to its MenuEntry object
    private final List<MenuEntry> menuEntries;

    @Inject
    public TopMenuExtension(@PluginName String name) {
        menuEntries = Lists.newArrayList();

        /*
         * By adding GerritTopMenu.PROJECTS, we are extending what is currently in the "Projects"
         * entry in the top menu with whatever MenuItems we add
         */
        menuEntries.add(new MenuEntry(GerritTopMenu.PROJECTS,
            Collections.singletonList(new MenuItem("Gerrit-CI", "#/x/" + name + "/settings"))));
    }

    @Override
    public List<MenuEntry> getEntries() {
        /*
         * NOTE: This is here for testing purposes because I don't have a better place to
         * put it.
         */
        JenkinsServerConfiguration jsc = new JenkinsServerConfiguration();
        try {
            jsc.setUri(new URI("http://localhost:8000"));
        } catch(URISyntaxException e) {}

        Map<String, Job> jobs = JenkinsProvider.getJobs(jsc);
        for(String key: jobs.keySet()) {
            try {
                JobWithDetails job = jobs.get(key).details();
                logger.info("Name: {} Url: {}", job.getName(), job.getUrl());
            } catch(IOException e) {
                logger.error("Error getting details for job: {}", key, e);
            }
        }

        VelocityProvider velocityProvider = new VelocityProvider();

        VelocityContext velocityContext = velocityProvider.getVelocityContext();
        velocityContext.put("stuff", "stuff");

        Template verifyTemplate =
            velocityProvider.getVelocityEngine().getTemplate(JobType.VERIFY.getTemplate());
        Template publishTemplate =
            velocityProvider.getVelocityEngine().getTemplate(JobType.PUBLISH.getTemplate());

        StringWriter xml = new StringWriter();
        verifyTemplate.merge(velocityContext, xml);
        String verifyJobConfig = xml.toString();

        xml = new StringWriter();
        publishTemplate.merge(velocityContext, xml);
        String publishJobConfig = xml.toString();

        JenkinsProvider.createJob(jsc, "group_repo_verify", verifyJobConfig);
        JenkinsProvider.createJob(jsc, "group_repo_publish", publishJobConfig);
        return menuEntries;
    }
}
