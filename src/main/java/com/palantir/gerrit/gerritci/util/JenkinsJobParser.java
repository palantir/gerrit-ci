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
package com.palantir.gerrit.gerritci.util;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import com.google.gson.JsonObject;
import com.palantir.gerrit.gerritci.constants.JobType;
import com.palantir.gerrit.gerritci.models.JenkinsServerConfiguration;
import com.palantir.gerrit.gerritci.providers.JenkinsProvider;

/**
 * This class handles the parsing of settings from Jenkins job XML configuration.
 */
public class JenkinsJobParser {

    private static final String GERRITPROJECT_TAG =
        "com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject";

    private static final String BRANCH_TAG =
        "com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.Branch";

    private static final String TIMEOUT_TAG = "hudson.plugins.build__timeout.BuildTimeoutWrapper";

    // Suppress default constructor for noninstantiability
    private JenkinsJobParser() {}

    /**
     * Given a Jenkins server and a project name, this method will query for the jobs' XML
     * configuration, parse all relevant settings and return them in a JsonObject that can be sent
     * to the front-end project settings screen.
     *
     * @param projectName The name of the project to get settings for.
     * @param jsc The Jenkins server that the project lives on.
     * @return A JsonObject containing all the settings that could be parsed.
     */
    public static JsonObject parseJenkinsJob(String projectName, JenkinsServerConfiguration jsc) {
        JsonObject settings = new JsonObject();
        for(JobType type: JobType.values()) {
            String jobName = type.getJobName(projectName);
            boolean exists = JenkinsProvider.jobExists(jsc, jobName);
            settings.addProperty(String.format("%sJobEnabled", type), exists);
            if(exists) {
                String jobXml = JenkinsProvider.getJobXml(jsc, jobName);
                settings.addProperty(String.format("%sBranchRegex", type), getBranchRegex(jobXml));
                settings.addProperty(String.format("%sCommand", type), getCommand(jobXml));
                settings.addProperty("timeoutMinutes", getTimeoutMinutes(jobXml));
                settings.addProperty("cronJob", getCronJob(jobXml));
                String junitPath = getJunitPath(jobXml);
                if (junitPath == "") {
                    settings.addProperty("junitEnabled", false);
                } else {
                    settings.addProperty("junitEnabled", true);
                    settings.addProperty("junitPath", junitPath);
                }
            }
        }

        return settings;
    }

    private static String getBranchRegex(String jobXml) {
        try {
            String branchRegex = Jsoup.parse(jobXml, "", Parser.xmlParser())
                    .getElementsByTag("gerritProjects").get(0)
                    .getElementsByTag(GERRITPROJECT_TAG).get(0)
                    .getElementsByTag("branches").get(0)
                    .getElementsByTag(BRANCH_TAG).get(0)
                    .getElementsByTag("pattern").get(0).html();

            // Remove "^" and "$" at the beginning and the end, respectively
            branchRegex = branchRegex.substring(1, branchRegex.length() - 1);

            // Remove sections of regex that we add post-user-input
            branchRegex = branchRegex.replace("(?!refs/meta/)", "");
            branchRegex = branchRegex.replace("(?!refs/)", "refs/heads/");

            return branchRegex;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static String getCommand(String jobXml) {
        try {
            String command = Jsoup.parse(jobXml, "", Parser.xmlParser())
                    .getElementsByTag("project").get(0)
                    .getElementsByTag("builders").get(0)
                    .getElementsByTag("hudson.tasks.Shell").get(0)
                    .getElementsByTag("command").get(0).html();
            return command.replaceAll("(?s)^.*### END PREBUILD-COMMANDS ###\\s+", "");
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static Integer getTimeoutMinutes(String jobXml) {
        try{
        return Integer.valueOf(Jsoup.parse(jobXml, "", Parser.xmlParser())
                .getElementsByTag(TIMEOUT_TAG).get(0)
                .getElementsByTag("strategy").get(0)
                .getElementsByTag("timeoutMinutes").get(0).html());
        }catch(IndexOutOfBoundsException e){
            return null;
        }
    }

    private static String getJunitPath(String jobXml){
        try {
            String junitPath = Jsoup.parse(jobXml, "", Parser.xmlParser())
                    .getElementsByTag("publishers").get(0)
                    .getElementsByTag("xunit").get(0)
                    .getElementsByTag("types").get(0)
                    .getElementsByTag("JUnitType").get(0)
                    .getElementsByTag("pattern").get(0).html();
            return junitPath;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static String getCronJob(String jobXml) {
        try {
            return Jsoup.parse(jobXml, "", Parser.xmlParser())
                    .getElementsByTag("triggers").get(0)
                    .getElementsByTag("hudson.triggers.TimerTrigger").get(0)
                    .getElementsByTag("spec").get(0).html();
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}
