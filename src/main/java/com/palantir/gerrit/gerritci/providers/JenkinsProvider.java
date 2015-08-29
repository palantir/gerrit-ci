//   Copyright 2015 Palantir Technologies
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
// 
//       http://www.apache.org/licenses/LICENSE-2.0
// 
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   
//   See the License for the specific language governing permissions and
//   limitations under the License.
package com.palantir.gerrit.gerritci.providers;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import com.palantir.gerrit.gerritci.constants.JobType;
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
        return new JenkinsServer(jsc.getUri(), jsc.getUsername(), jsc.getPassword());
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
     * @param name The name of the job to get the XML for.
     * @return The XML configuration for the specified Jenkins job.
     * @throws RuntimeException if the get operation failed.
     */
    public static String getJobXml(JenkinsServerConfiguration jsc, String name) {
        JenkinsServer server = getJenkinsServer(jsc);
        try {
            return server.getJobXml(name);
        } catch(IOException e) {
            throw new RuntimeException(
                String.format("Error getting job XML from Jenkins for job: %s", name), e);
        }
    }

    /**
     * Queries the specified Jenkins server to see if the specified job exists.
     *
     * @param jsc The Jenkins server which we are querying.
     * @param name The name of the job to check.
     * @return Whether or not the job exists on the specified server.
     */
    public static boolean jobExists(JenkinsServerConfiguration jsc, String name) {
        JenkinsServer server = getJenkinsServer(jsc);
        try {
            return server.getJob(name) != null;
        } catch(IOException e) {
            throw new RuntimeException("Error checking for job from Jenkins", e);
        }
    }

    /**
     * Creates a new job on the specified Jenkins server with the specified name and configuration,
     * or updates the job with the specified name if it already exists on the server.
     *
     * @param jsc The Jenkins server to add the new job to.
     * @param name The name of the job to add.
     * @param type The JobType of the job to add.
     * @param params The configuration parameters for the new job.
     * @throws RuntimeException if the job wasn't created for other reasons.
     */
    public static void createOrUpdateJob(JenkinsServerConfiguration jsc, String name, JobType type,
        Map<String, Object> params) {
        JenkinsServer server = getJenkinsServer(jsc);

        VelocityProvider velocityProvider = new VelocityProvider();
        VelocityContext velocityContext = velocityProvider.getVelocityContext(params);

        Template template = velocityProvider.getVelocityEngine().getTemplate(type.getTemplate());

        StringWriter xml = new StringWriter();
        template.merge(velocityContext, xml);
        String jobXml = xml.toString();

        if(jobExists(jsc, name)) {
            try {
                server.updateJob(name, jobXml, false);
            } catch(IOException e) {
                throw new RuntimeException(String.format("Failed to update Jenkins job: %s", name),
                    e);
            }
        } else {
            try {
                server.createJob(name, jobXml, false);
            } catch(IOException e) {
                throw new RuntimeException(String.format("Failed to create Jenkins job: %s", name),
                    e);
            }
        }
    }

    /**
     * Deletes the specified job from the specified Jenkins server.
     *
     * @param jsc The Jenkins server to delete the job from.
     * @param name The name of the job to delete.
     */
    public static void deleteJob(JenkinsServerConfiguration jsc, String name) {
        JenkinsServer server = getJenkinsServer(jsc);

        // If the job doesn't exist, there is nothing left to do
        if(!jobExists(jsc, name)) {
            return;
        }

        try {
            server.deleteJob(name, false);
        } catch(IOException e) {
            throw new RuntimeException(String.format("Error deleting job %s from Jenkins", name), e);
        }
    }
}
