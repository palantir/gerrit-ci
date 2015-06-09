package com.palantir.gerrit.gerritci;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;

public class JenkinsManager {
    private static final Logger logger = LoggerFactory.getLogger(JenkinsManager.class);

    /**
     * Queries the Jenkins server for all existing jobs.
     * 
     * @return A mapping from job name to Job object
     */
    public static Map<String, Job> getJobs() {
        JenkinsServer server;
        try {
            server = new JenkinsServer(new URI("http://localhost:8000"));
        } catch(URISyntaxException e) {
            logger.error("Error getting jobs from Jenkins", e);
            return null;
        }

        try {
            return server.getJobs();
        } catch(IOException e) {
            logger.error("Error getting jobs from Jenkins", e);
            return null;
        }
    }
}
