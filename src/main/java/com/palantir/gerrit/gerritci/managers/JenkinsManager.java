package com.palantir.gerrit.gerritci.managers;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import com.palantir.gerrit.gerritci.models.JenkinsServerConfiguration;

public class JenkinsManager {
    private static final Logger logger = LoggerFactory.getLogger(JenkinsManager.class);

    private static JenkinsServer getJenkinsServer(JenkinsServerConfiguration jsc) {
        return new JenkinsServer(jsc.getUri());
    }

    /**
     * Queries a Jenkins server for all existing jobs.
     * 
     * @param jsc The Jenkins server for which we are querying all jobs.
     * @return A mapping from job name to Job object
     */
    public static Map<String, Job> getJobs(JenkinsServerConfiguration jsc) {
        try {
            return getJenkinsServer(jsc).getJobs();
        } catch(IOException e) {
            logger.error("Error getting jobs from Jenkins", e);
            //TODO: Throw runtime exception instead of return null.
            return null;
        }
    }

    /**
     * Gets the XML configuration for a Jenkins job.
     * 
     * @param jsc The Jenkins server for which we are querying the job XML.
     * @param jobName The name of the job to get the XML for.
     * @return The XML configuration for the specified Jenkins job.
     */
    public static String getJobXml(JenkinsServerConfiguration jsc, String jobName) {
        JenkinsServer server = getJenkinsServer(jsc);
        try {
            return server.getJobXml(jobName);
        } catch(IOException e) {
            logger.error("Error getting job XML from Jenkins for Job: {}", jobName, e);
            //TODO: Throw runtime exception instead of return null.
            return null;
        }
    }
}
