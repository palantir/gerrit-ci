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
package com.palantir.gerrit.gerritci.servlets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.JarFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.google.gerrit.common.data.GerritConfig;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.offbytwo.jenkins.JenkinsServer;
import com.palantir.gerrit.gerritci.constants.JobType;
import com.palantir.gerrit.gerritci.models.JenkinsServerConfiguration;
import com.palantir.gerrit.gerritci.providers.JenkinsProvider;
import com.palantir.gerrit.gerritci.util.JenkinsJobParser;

@Singleton
public class JobsServlet extends HttpServlet {

    private static final long serialVersionUID = -4428173510340797397L;
    private ProjectControl.Factory projectControlFactory;
    private GerritConfig gerritConfig;
    private String canonicalWebUrl;
    private SitePaths sitePaths;
    private static final Logger logger = LoggerFactory.getLogger(JobsServlet.class);

    @Inject
    public JobsServlet(final ProjectControl.Factory projectControlFactory,
                       final GerritConfig gerritConfig, @CanonicalWebUrl String canonicalWebUrl,
                       final SitePaths sitePaths) {
        this.projectControlFactory = projectControlFactory;
        this.gerritConfig = gerritConfig;
        this.canonicalWebUrl = canonicalWebUrl;
        this.sitePaths = sitePaths;
    }

    private int getResponseCode(String projectName) {
        try {
            ProjectControl projectControl =
                this.projectControlFactory.controlFor(new NameKey(projectName));
            CurrentUser user = projectControl.getCurrentUser();

            // This will be the case if the user is unauthenticated.
            if(user.getRealUser().toString().equals("ANONYMOUS")) {
                return 401;
            }

            // Make sure the user is the owner of the project or an admin.
            if(!(projectControl.isVisible() && (user.getCapabilities().canAdministrateServer() || projectControl
                .isOwner()))) {
                return 403;
            }

            return 200;
        } catch(NoSuchProjectException e) {
            return 404;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException,
        IOException {
        String encodedProjectName =
            req.getRequestURI().substring(req.getRequestURI().lastIndexOf('/') + 1);
        String projectName = encodedProjectName.replace("%2F", "/");

        //Always send 200 status and handle errors in ProjectScreenSettings
        res.setStatus(200);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        //When an error occurs, this returns an error message to ProjectScreenSettings to warn the user.
        if(!safetyCheck(getResponseCode(projectName), res, projectName))
            return;

        FileBasedConfig cfg =
                new FileBasedConfig(new File(sitePaths.etc_dir, "gerrit-ci.config"), FS.DETECTED);
        try {
            cfg.load();
        } catch(ConfigInvalidException e) {
            logger.error("Error loading config file after get request:", e);
            JsonObject errorMsg = makeErrorJobObject(connectionError);
            res.getWriter().write(errorMsg.toString());
            return;
        }

        String jenkinsUrlString = cfg.getString("Settings", "Jenkins", "jenkinsURL");
        String jenkinsUserString = cfg.getString("Settings", "Jenkins", "jenkinsUser");
        String jenkinsPasswordString = cfg.getString("Settings", "Jenkins", "jenkinsPassword");

        JenkinsServerConfiguration jsc = new JenkinsServerConfiguration();

        try {
            jsc.setUri(new URI(jenkinsUrlString));
        } catch (Exception e) {
            logger.error("Error loading config file after get request:", e);
            JsonObject errorMsg = makeErrorJobObject(connectionError);
            res.getWriter().write(errorMsg.toString());
            return;
        }
        jsc.setUsername(jenkinsUserString);
        jsc.setPassword(jenkinsPasswordString);
        try {
            ArrayList<String> jobNames = getJenkinJobs(projectName);
            Map<String, JsonArray> jobs = new HashMap<String, JsonArray>();
            for (String jobName : jobNames) {
                String jobType = getTypeFromName(jobName);
                JsonArray params = JenkinsJobParser.parseJenkinsJob(jobName, jobType, jsc);
                jobs.put(jobName, params);
            }
            JsonObject returnObj = makeJSonRequest(jobs);
            res.getWriter().write(returnObj.toString());
        } catch (RuntimeException e) {
            logger.error("Error checking job from Jenkins:", e);
            JsonObject errorMsg = makeErrorJobObject(connectionError);
            res.getWriter().write(errorMsg.toString());
        }
    }

    private static String getTypeFromName(String name) {
        if (name.startsWith("cron"))
            return "cron";
        else if (name.startsWith("publish"))
            return "publish";
        else if (name.startsWith("verify"))
            return "verify";
        else
            return "UNKNOWN";
    }

    // Returns a list of gerrit-ci created jobs for the project that havn't
    // been deleted yet on gerrit-ci
    private ArrayList<String> getJenkinJobs(String projectName) throws IOException {
        ArrayList<String> jobs = new ArrayList<String>();
        File projectConfigDirectory = new File(sitePaths.etc_dir, projectName);
        if (!projectConfigDirectory.exists())
            projectConfigDirectory.mkdir();
        File projectConfigFile = new File(projectConfigDirectory, "created_jobs");
        if (!projectConfigFile.exists())
            projectConfigFile.createNewFile();
        Scanner scanner = new Scanner(projectConfigFile);
        while (scanner.hasNext()) {
            String line = scanner.next();
            jobs.add(line);
        }
        scanner.close();
        return jobs;
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException,
        IOException {
        String encodedProjectName =
            req.getRequestURI().substring(req.getRequestURI().lastIndexOf('/') + 1);
        String projectName = encodedProjectName.replace("%2F", "/");

        res.setStatus(200);
        res.setContentType("text/plain");

        //When an error occurs, returns error message to ProjectScreenSettings
        if(!safetyCheck(getResponseCode(projectName), res, projectName))
            return;

        FileBasedConfig cfg = new FileBasedConfig(new File(sitePaths.etc_dir, "gerrit-ci.config"), FS.DETECTED);
        try {
            cfg.load();
        } catch (ConfigInvalidException | IOException e) {
            logger.error("Error loading config file after get request:", e);
        }

        Map<String, Map<String, String>> jobsToParams = new HashMap<String, Map<String, String>>();
        try {
            jobsToParams = parseJobRequest(req, projectName);
        } catch (JsonSyntaxException | NoSuchProjectException e) {
            logger.error("Failed to parse job request:", e);
        }

        Map<String, Object> serverParams = getJenkinsSpecificParams(cfg);
        JenkinsServerConfiguration jsc = new JenkinsServerConfiguration();

        String jenkinsUrlString = cfg.getString("Settings", "Jenkins", "jenkinsURL");
        String jenkinsUserString = cfg.getString("Settings", "Jenkins", "jenkinsUser");
        String jenkinsPasswordString = cfg.getString("Settings", "Jenkins", "jenkinsPassword");

        try {
            jsc.setUri(new URI(jenkinsUrlString));
        } catch (Exception e) {
            logger.error("Error setting url " + jenkinsUrlString, e);
        }
        jsc.setUsername(jenkinsUserString);
        jsc.setPassword(jenkinsPasswordString);

        for (String jobName : jobsToParams.keySet()) {
            if (jobsToParams.get(jobName) == null) {
                logger.info("Deleting job: " + jobName);
                JenkinsProvider.deleteJob(jsc, jobName);
            }
            // get rid of "" that begin and end parsed jobname and type

            else {
                String jobPre = jobName.substring(0, 4);
                Map<String, Object> params = new HashMap<String, Object>();
                params.putAll(serverParams);
                for (String field : jobsToParams.get(jobName).keySet()) {
                    Object val = jobsToParams.get(jobName).get(field);
                    if(field.endsWith("Regex"))
                        val = parseBranchRegex((String) val);
                    if(field.equals("timeoutMinutes")){
                        try{
                        val = Integer.parseInt((String) val);}
                        catch(NumberFormatException e){
                            val = 30;
                        }
                        logger.info("putting timeout minutes: " + val);
                    }
                    params.put(field, val);
                }
                if (jobPre.equals("cron")) {
                    createOrUpdateJob(jsc, jobName, JobType.CRON, params);
                } else if (jobPre.equals("publ")) {
                    createOrUpdateJob(jsc, jobName, JobType.PUBLISH, params);
                } else if (jobPre.equals("veri")) {
                    createOrUpdateJob(jsc, jobName, JobType.VERIFY, params);
                }
            }
        }
    }

    public static boolean safetyCheck(int responseCode, HttpServletResponse res, String projectName) throws IOException {
        if (responseCode != 200) {
            if (responseCode == 404) {
                logger.error("Could not find project with name: " + projectName);
                JsonObject errorMsg = makeErrorJobObject("Could not find project with name: " + projectName);
                res.getWriter().write(errorMsg.toString());
            } else {
                logger.error("User Authentication Error ");
                JsonObject errorMsg = makeErrorJobObject("Permission Denied: User Authentication Error ");
                res.getWriter().write(errorMsg.toString());
            }
            return false;
        }
        return true;
    }

    public Map<String, Object> getJenkinsSpecificParams(FileBasedConfig cfg) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();

        // publishJobEnabled
        if(!requestParams.has("publishJobEnabled")) {
            res.setStatus(400);
            return;
        }
        boolean publishJobEnabled =
            requestParams.get("publishJobEnabled").getAsJsonObject().get("b").getAsBoolean();

        String sshPort = gerritConfig.getSshdAddress();
        sshPort = sshPort.substring(sshPort.lastIndexOf(':') + 1);

        String host = canonicalWebUrl.replace("https://", "").replace("http://", "");
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(':'));
        }
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }

        params.put("gerritUser", cfg.getString("Settings", "Jenkins", "gerritUser"));
        params.put("host", host);
        params.put("port", sshPort);
        params.put("credentialsId", cfg.getString("Settings", "Jenkins", "credentialsId"));
        return params;
    }

    public static JsonObject makeJSonRequest(Map<String, JsonArray> jobs) {
        JsonArray obj = new JsonArray();
        for (String jobName : jobs.keySet()) {
            JsonObject jobObject = new JsonObject();
            jobObject.addProperty("jobName", jobName);
            jobObject.addProperty("jobType", getTypeFromName(jobName));
            jobObject.add("items", jobs.get(jobName));
            obj.add(jobObject);
        }
        JsonObject objWrapper = new JsonObject();
        objWrapper.add("items", obj);
        return objWrapper;
    }

    // This parses the jobRequest and updates the config file for the project.
    // It the creates job config files for new jobs
    // and marks files of deleted jobs with "DELETED"

    public Map<String, Map<String, String>> parseJobRequest(HttpServletRequest req, String projectName) throws JsonSyntaxException,
            IOException, NoSuchProjectException {
        Map<String, Map<String, String>> jobToParams = new HashMap<String, Map<String, String>>();

        File projectConfigDirectory = new File(sitePaths.etc_dir, projectName);
        if (!projectConfigDirectory.exists())
            projectConfigDirectory.mkdir();
        File projectConfigFile = new File(projectConfigDirectory, "created_jobs");
        if (!projectConfigFile.exists())
            projectConfigFile.createNewFile();

        JsonObject requestBody = (JsonObject) (new JsonParser()).parse(CharStreams.toString(req.getReader()));

        // get number of jobs
        // If all jobs are deleted, we must purge jobs
        int numOfJobs = requestBody.get("items").getAsJsonArray().size();

        ArrayList<String> receivedJobNames = new ArrayList<String>();

        if (numOfJobs < 1) {
            ArrayList<String> deletedJobs = updateProjectJobFiles(projectConfigFile, projectConfigDirectory, receivedJobNames);
            for (String deleted : deletedJobs) {
                jobToParams.put(deleted, null);
            }
            return jobToParams;
        }

        // for each received job, create or rewrite its config file and add to
        // jobToParams
        for (int i = 0; i < numOfJobs; i++) {
            JsonObject jobObject = requestBody.get("items").getAsJsonArray().get(i).getAsJsonObject();
            String jobName = jobObject.get("jobName").toString();
            jobName = jobName.substring(1, jobName.length() - 1);
            receivedJobNames.add(jobName);
            String type = jobObject.get("jobType").toString();
            type = type.substring(1, type.length() - 1);
            int numOfParams = jobObject.get("items").getAsJsonArray().size();
            JsonArray paramsArray = jobObject.get("items").getAsJsonArray();
            FileBasedConfig jobConfig = makeJobConfigFile(projectConfigDirectory, jobName,
                    this.projectControlFactory.controlFor(new NameKey(projectName)).getCurrentUser());
            Map<String, String> parsedParams = new HashMap<String, String>();
            parsedParams.put("projectName", projectName);
            // updating the job config file and storing job info in params map
            for (int j = 0; j < numOfParams; j++) {
                String field = paramsArray.get(j).getAsJsonObject().get("field").toString();
                field = field.substring(1, field.length() - 1);
                String value = paramsArray.get(j).getAsJsonObject().get("value").toString();
                value = value.substring(1, value.length() - 1);
                parsedParams.put(field, value);
                // update jobconfig files
                jobConfig.setString("jobType", type, field, value);
            }
            jobConfig.save();

            jobToParams.put(jobName, parsedParams);
        }
        // update or create project files for all jobs
        ArrayList<String> deletedJobs = updateProjectJobFiles(projectConfigFile, projectConfigDirectory, receivedJobNames);
        for (String deleted : deletedJobs) {
            jobToParams.put(deleted, null);
        }
        // returns map of job name to params
        return jobToParams;
    }

    public static ArrayList<String> updateProjectJobFiles(File projectFile, File projectConfigDirectory, ArrayList<String> receivedJobNames)
            throws IOException {

        Scanner scanner = new Scanner(projectFile);

        ArrayList<String> updatedJobs = new ArrayList<String>();
        ArrayList<String> newJobs = new ArrayList<String>();
        ArrayList<String> deletedJobs = new ArrayList<String>();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (receivedJobNames.contains(line)) {
                updatedJobs.add(line);
                receivedJobNames.remove(line);
            } else {
                deletedJobs.add(line);
            }
        }
        logger.info("There are " + receivedJobNames.size() + " new jobs");
        logger.info("There are " + deletedJobs.size() + " deleted jobs");
        logger.info("There are " + updatedJobs.size() + " updated jobs");
        for (String s : receivedJobNames) {
            newJobs.add(s);
        }

        scanner.close();
        FileWriter writer = new FileWriter(projectFile, false);
        for (String s : updatedJobs) {
            writer.write(s);
            writer.write("\r\n");
        }
        for (String s : newJobs) {
            writer.write(s);
            writer.write("\r\n");
        }
        writer.close();
        for (File f : projectConfigDirectory.listFiles()) {
            String filename = f.getName();
            if (deletedJobs.contains(filename.substring(0, filename.length() - 7))) {
                File deleted = new File("DELETED_" + filename);
                deleted.createNewFile();
            }
        }
        return deletedJobs;
    }

    public static FileBasedConfig makeJobConfigFile(File etc_dir, String jobName, CurrentUser currentUser) {
        File confFile = new File(etc_dir, jobName + ".config");
        FileBasedConfig cfg = new FileBasedConfig(confFile, FS.DETECTED);
        try {
            cfg.load();
        } catch (ConfigInvalidException | IOException e) {
            logger.error("Received PUT request. Error loading project job file:", e);
        }
        cfg.clear();
        cfg.setString("JobName", jobName, "UserUpdated", currentUser.getUserName());
        return cfg;
    }

    public static boolean safetyCheck(int responseCode, HttpServletResponse res, String projectName) throws IOException{
        if(responseCode != 200) {
            JsonObject errorMsg = new JsonObject();
            if(responseCode == 404){
                logger.error("Could not find project with name: " + projectName);
                errorMsg.addProperty("error", "Could not find project with name: " + projectName);
                res.getWriter().write(errorMsg.toString());
            }
            else {
                    logger.error("User Authentication Error ");
                    errorMsg.addProperty("error", "Permission Denied: User Authentication Error ");
                    res.getWriter().write(errorMsg.toString());
            }
            return false;
        }

    private String parseBranchRegex(String s){
        if(s.startsWith("refs/heads/")) {
            s = s.replace("refs/heads/", "(?!refs/)");
        }
        s = String.format("(?!refs/meta/)%s", s);
        s = String.format("^%s$", s);
        return s;
    }
}
