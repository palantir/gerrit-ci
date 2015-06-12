package com.palantir.gerrit.gerritci.providers;

import java.io.IOException;
import java.util.Map;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import com.palantir.gerrit.gerritci.models.JenkinsServerConfiguration;

/**
 * This class provides a set of methods that are higher-level abstractions of the JenkinsServer
 * methods. Exceptions in the JenkinsServer methods are caught and re-thrown as runtime exceptions
 * with user-friendly error messages. All methods in this class are static. This class is not
 * meant to be instantiated.
 */
public class JenkinsProvider {

    // Suppress default constructor for noninstantiability
    private JenkinsProvider() {}

    // Returns a new JenkinsServer instance based off of the provided server configuration
    private static JenkinsServer getJenkinsServer(JenkinsServerConfiguration jsc) {
        return new JenkinsServer(jsc.getUri());
    }

    /**
     * Queries a Jenkins server for all existing jobs.
     *
     * @param jsc The Jenkins server for which we are querying all jobs.
     * @return A mapping from job name to Job object of all jobs on the server.
     * @throws RuntimeException if the get operation failed.
     */
    public static Map<String, Job> getJobs(JenkinsServerConfiguration jsc) {
        try {
            return getJenkinsServer(jsc).getJobs();
        } catch(IOException e) {
            throw new RuntimeException("Error getting jobs from Jenkins", e);
        }
    }

    /**
     * Gets the XML configuration for a Jenkins job.
     *
     * @param jsc The Jenkins server for which we are querying the job XML.
     * @param jobName The name of the job to get the XML for.
     * @return The XML configuration for the specified Jenkins job.
     * @throws RuntimeException if the get operation failed.
     */
    public static String getJobXml(JenkinsServerConfiguration jsc, String jobName) {
        JenkinsServer server = getJenkinsServer(jsc);
        try {
            return server.getJobXml(jobName);
        } catch(IOException e) {
            throw new RuntimeException(String.format(
                "Error getting job XML from Jenkins for job: %s", jobName), e);
        }
    }

    /**
     * Creates a new job on the specified Jenkins server with the specified name and configuration.
     *
     * @param jsc The Jenkins server to add the new job to.
     * @param jobName The name of the job to add.
     * @param jobXml The configuration XML for the new job.
     * @throws IllegalArgumentException if the job already exists on the server.
     * @throws RuntimeException if the job wasn't created for other reasons.
     */
    public static void createJob(JenkinsServerConfiguration jsc, String jobName, String jobXml) {
        JenkinsServer server = getJenkinsServer(jsc);

        Job job = null;
        try {
            job = server.getJob(jobName);
        } catch(IOException e) {
            throw new RuntimeException(String.format("Failed to test if Jenkins job: %s exists",
                jobName), e);
        }

        if(job != null) {
            throw new IllegalArgumentException(String.format("Job %s already exists on the server",
                jobName));
        }

        try {
            server.createJob(jobName, jobXml);
        } catch(IOException e) {
            throw new RuntimeException(String.format("Failed to create Jenkins job: %s", jobName),
                e);
        }
    }
}
