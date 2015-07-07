package com.palantir.gerrit.gerritci.util;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

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

    private static final String COMMAND_PREFIX =
        "#!/bin/bash set -x set -e date git reset --hard git clean -fdx ";

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

        String verifyJobName = JobType.VERIFY.getJobName(projectName);
        String publishJobName = JobType.PUBLISH.getJobName(projectName);

        boolean verifyExists = JenkinsProvider.jobExists(jsc, verifyJobName);
        settings.addProperty("verifyJobEnabled", verifyExists);
        if(verifyExists) {
            String verifyJobXml = JenkinsProvider.getJobXml(jsc, verifyJobName);

            String verifyBranchRegex = Jsoup.parse(verifyJobXml, "", Parser.xmlParser())
                                     .getElementsByTag("gerritProjects").get(0)
                                     .getElementsByTag(GERRITPROJECT_TAG).get(0)
                                     .getElementsByTag("branches").get(0)
                                     .getElementsByTag(BRANCH_TAG).get(0)
                                     .getElementsByTag("pattern").get(0).html();
            verifyBranchRegex = verifyBranchRegex.substring(1,verifyBranchRegex.length() - 1);
            verifyBranchRegex = verifyBranchRegex.replace("(?!refs/meta/)", "");
            verifyBranchRegex = verifyBranchRegex.replace("(?!refs/)", "refs/heads/");
            settings.addProperty("verifyBranchRegex", verifyBranchRegex);

            String verifyCommand =
                Jsoup.parse(verifyJobXml, "", Parser.xmlParser()).getElementsByTag("project")
                    .get(0).getElementsByTag("builders").get(0)
                    .getElementsByTag("hudson.tasks.Shell").get(0).getElementsByTag("command")
                    .get(0).html();
            settings.addProperty("verifyCommand", verifyCommand.replace(COMMAND_PREFIX, ""));

            Elements timeoutTags =
                Jsoup.parse(verifyJobXml, "", Parser.xmlParser()).getElementsByTag(TIMEOUT_TAG);
            boolean timeoutEnabled = timeoutTags.size() > 0;
            settings.addProperty("timeoutEnabled", timeoutEnabled);

            if(timeoutEnabled) {
                settings.addProperty("timeoutMinutes",
                                     Integer.valueOf(timeoutTags.get(0)
                                         .getElementsByTag("strategy").get(0)
                                         .getElementsByTag("timeoutMinutes").get(0).html()));
            }
        }

        boolean publishExists = JenkinsProvider.jobExists(jsc, publishJobName);
        settings.addProperty("publishJobEnabled", publishExists);
        if(publishExists) {
            String publishJobXml = JenkinsProvider.getJobXml(jsc, publishJobName);

            String publishBranchRegex = Jsoup.parse(publishJobXml, "", Parser.xmlParser())
                                     .getElementsByTag("gerritProjects").get(0)
                                     .getElementsByTag(GERRITPROJECT_TAG).get(0)
                                     .getElementsByTag("branches").get(0)
                                     .getElementsByTag(BRANCH_TAG).get(0)
                                     .getElementsByTag("pattern").get(0).html();
            publishBranchRegex = publishBranchRegex.substring(1, publishBranchRegex.length() - 1);
            publishBranchRegex = publishBranchRegex.replace("(?!refs/)", "refs/heads/");
            settings.addProperty("publishBranchRegex", publishBranchRegex);

            String publishCommand =
                Jsoup.parse(publishJobXml, "", Parser.xmlParser()).getElementsByTag("project")
                    .get(0).getElementsByTag("builders").get(0)
                    .getElementsByTag("hudson.tasks.Shell").get(0).getElementsByTag("command")
                    .get(0).html();
            settings.addProperty("publishCommand", publishCommand.replace(COMMAND_PREFIX, ""));

            Elements timeoutTags =
                Jsoup.parse(publishJobXml, "", Parser.xmlParser()).getElementsByTag(TIMEOUT_TAG);
            boolean timeoutEnabled = timeoutTags.size() > 0;
            settings.addProperty("timeoutEnabled", timeoutEnabled);

            if(timeoutEnabled) {
                settings.addProperty("timeoutMinutes",
                                     Integer.valueOf(timeoutTags.get(0)
                                         .getElementsByTag("strategy").get(0)
                                         .getElementsByTag("timeoutMinutes").get(0).html()));
            }
        }

        return settings;
    }
}
