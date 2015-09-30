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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.jar.JarFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.google.gerrit.common.data.GerritConfig;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
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
import com.palantir.gerrit.gerritci.util.ConfigFileUtils;
import com.palantir.gerrit.gerritci.util.JenkinsJobParser;

@Singleton
public class JobsServlet extends HttpServlet {

    private static final long serialVersionUID = -4428173510340797397L;
    private ProjectControl.Factory projectControlFactory;
    private GerritConfig gerritConfig;
    private String canonicalWebUrl;
    private SitePaths sitePaths;
    private static final Logger logger = LoggerFactory.getLogger(JobsServlet.class);
    private static final String connectionError = "Please verify connection to Jenkins " +
            "is valid in the gerrit-ci settings page " +
            "(administrator access is required).";

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
        try {
            JenkinsServerConfiguration jsc = ConfigFileUtils.getJenkinsConfigFromFile(new File(sitePaths.etc_dir, "gerrit-ci.config"));
            ArrayList<String> jobNames = ConfigFileUtils.getJobsFromFile(new File(sitePaths.etc_dir, projectName));
            Map<String, JsonArray> jobs = new HashMap<String, JsonArray>();
            for (String jobName : jobNames) {
                String jobType = getTypeFromName(jobName);
                JsonArray params = JenkinsJobParser.parseJenkinsJob(jobName, jobType, jsc);
                jobs.put(jobName, params);
            }
            JsonObject returnObj = makeJSonRequest(jobs);
            res.getWriter().write(returnObj.toString());
        } catch (Exception e) {
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

        Map<String, Map<String, String>> jobsToParams = new HashMap<String, Map<String, String>>();
        try {
            jobsToParams = parseJobRequest(req, projectName);
        } catch (JsonSyntaxException | NoSuchProjectException | GitAPIException e) {
            logger.error("Failed to parse job request:", e);
        }

        File projectConfigFile = new File(sitePaths.etc_dir, "gerrit-ci.config");
        Map<String, Object> serverParams = ConfigFileUtils.getJenkinsSpecificParams(projectConfigFile, gerritConfig, canonicalWebUrl);
        JenkinsServerConfiguration jsc = ConfigFileUtils.getJenkinsConfigFromFile(projectConfigFile);

        for (String jobName : jobsToParams.keySet()) {
            if (jobsToParams.get(jobName) == null) {
                logger.info("Deleting job: " + jobName);
                JenkinsProvider.deleteJob(jsc, jobName);
            }

            else {
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
                    }
                    params.put(field, val);
                }
                if (jobName.startsWith("cron")) {
                    createOrUpdateJob(jsc, jobName, JobType.CRON, params);
                } else if (jobName.startsWith("publish")) {
                    createOrUpdateJob(jsc, jobName, JobType.PUBLISH, params);
                } else if (jobName.startsWith("verify")) {
                    createOrUpdateJob(jsc, jobName, JobType.VERIFY, params);
                }
            }
        }
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
            IOException, NoSuchProjectException, NoFilepatternException, GitAPIException {
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

        CurrentUser currentUser = this.projectControlFactory.controlFor(new NameKey(projectName)).getCurrentUser();
        String gitPath = getGitPath(sitePaths);
        File gitDir = new File(gitPath, projectName + ".git");
        Repository repository = new FileRepositoryBuilder().setGitDir(gitDir).build();
        ObjectInserter objectInserter = repository.newObjectInserter();
        HashMap<String, ObjectId> jobsToIds = new HashMap<String, ObjectId>();
        // assign file name and append to tree
        TreeFormatter treeFormatter = new TreeFormatter();
        // for each received job, create or rewrite its config file and add to
        // jobToParams
        for (int i = 0; i < numOfJobs; i++) {
            JsonObject jobObject = requestBody.get("items").getAsJsonArray().get(i).getAsJsonObject();
            String jobName = jobObject.get("jobName").toString();
            //Remove leading and trailing quotations ex. "jobname" becomes jobname
            jobName = jobName.substring(1, jobName.length() - 1);
            receivedJobNames.add(jobName);
            String type = jobObject.get("jobType").toString();
            type = type.substring(1, type.length() - 1);
            int numOfParams = jobObject.get("items").getAsJsonArray().size();
            JsonArray paramsArray = jobObject.get("items").getAsJsonArray();
            FileBasedConfig jobConfig = makeJobConfigFile(projectConfigDirectory, jobName, currentUser);
            Map<String, String> parsedParams = new HashMap<String, String>();
            parsedParams.put("projectName", projectName);
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
            jobsToIds.put(jobName, createGitFileId(repository, jobConfig, objectInserter, jobName));
            jobToParams.put(jobName, parsedParams);
        }
        for (String jobName : jobsToIds.keySet()) {
            treeFormatter.append(jobName + ".config", FileMode.REGULAR_FILE, jobsToIds.get(jobName));
        }
        ObjectId treeId = objectInserter.insert(treeFormatter);
        objectInserter.flush();
        updateProjectRef(treeId, objectInserter, repository, currentUser);
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
            writer.write("\n");
        }
        for (String s : newJobs) {
            writer.write(s);
            writer.write("\n");
        }
        writer.close();
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

    public static JsonObject makeErrorJobObject(String errorMessage) {
        JsonArray jobsItemWrapper = new JsonArray();
        JsonObject jenkinsJobWrapper = new JsonObject();
        jenkinsJobWrapper.addProperty("jobName", "ERROR");
        jenkinsJobWrapper.addProperty("jobType", "ERROR");
        JsonArray jenkinsJobItemsWrapper = new JsonArray();
        JsonObject errorMessageWrapper = new JsonObject();
        errorMessageWrapper.addProperty("field", "errorMessage");
        errorMessageWrapper.addProperty("value", errorMessage);
        jenkinsJobItemsWrapper.add(errorMessageWrapper);
        jenkinsJobWrapper.add("items", jenkinsJobItemsWrapper);
        JsonObject objWrapper = new JsonObject();
        jobsItemWrapper.add(jenkinsJobWrapper);
        objWrapper.add("items", jobsItemWrapper);
        return objWrapper;
    }

    public static boolean safetyCheck(int responseCode, HttpServletResponse res, String projectName) throws IOException{
        if(responseCode != 200) {
            JsonObject errorMsg = new JsonObject();
            if(responseCode == 404){
                logger.error("Could not find project with name: " + projectName);
                errorMsg = makeErrorJobObject("Could not find project with name: " + projectName);
                res.getWriter().write(errorMsg.toString());
            } else {
                logger.error("User Authentication Error ");
                errorMsg = makeErrorJobObject("Permission Denied: User Authentication Error ");
                res.getWriter().write(errorMsg.toString());
            }
            return false;
        }
        return true;
    }

    public static String getGitPath(SitePaths sitePaths) throws IOException {
        FileBasedConfig cfg = new FileBasedConfig(new File(sitePaths.etc_dir, "gerrit.config"), FS.DETECTED);
        try {
            cfg.load();
        } catch (ConfigInvalidException e) {
            logger.error("Received GET Request. Error loading gerrit-ci.config file:", e);
        }
        return cfg.getString("gerrit", null, "basePath");
    }

    public static ObjectId createGitFileId(Repository repository, FileBasedConfig jobConfig, ObjectInserter objectInserter, String jobName) throws UnsupportedEncodingException, IOException{
        ObjectId fileId = objectInserter.insert(Constants.OBJ_BLOB, jobConfig.toText().getBytes("utf-8"));
        objectInserter.flush();
        logger.info("Created blobId " + fileId + " for " + jobName);
        return fileId;
    }

    public static void updateProjectRef(ObjectId treeId, ObjectInserter objectInserter, Repository repository, CurrentUser currentUser)
            throws IOException, NoFilepatternException, GitAPIException {
        // Create a branch
        Ref gerritCiRef = repository.getRef("refs/meta/gerrit-ci");
        CommitBuilder commitBuilder = new CommitBuilder();
        commitBuilder.setTreeId(treeId);
        logger.info("treeId: " + treeId);

        if (gerritCiRef != null) {
            ObjectId prevCommit = gerritCiRef.getObjectId();
            logger.info("prevCommit: " + prevCommit);
            commitBuilder.setParentId(prevCommit);
        }
        // build commit
        logger.info("Adding git tree : " + treeId);
        commitBuilder.setMessage("Modify project build rules.");
        final IdentifiedUser iUser = (IdentifiedUser) currentUser;
        PersonIdent user = new PersonIdent(currentUser.getUserName(), iUser.getEmailAddresses().iterator().next());
        commitBuilder.setAuthor(user);
        commitBuilder.setCommitter(user);
        ObjectId commitId = objectInserter.insert(commitBuilder);
        objectInserter.flush();
        logger.info(" Making new commit: " + commitId);
        RefUpdate newRef = repository.updateRef("refs/meta/gerrit-ci");
        newRef.setNewObjectId(commitId);
        newRef.update();
        repository.close();
    }

    /**
     * Creates a new job on the specified Jenkins server with the specified name and configuration,
     * or updates the job with the specified name if it already exists on the server.
     *
     * @param jsc The Jenkins server to add the new job to.
     * @param name The name of the job to add.
     * @param type The JobType of the job to add.
     * @param params The configuration parameters for the new job.
     * @throws IOException
     * @throws RuntimeException if the job wasn't created for other reasons.
     */
    public void createOrUpdateJob(JenkinsServerConfiguration jsc, String name, JobType type, Map<String, Object> params) throws IOException {
        JenkinsServer server = JenkinsProvider.getJenkinsServer(jsc);
        VelocityContext velocityContext = new VelocityContext(params);
        StringWriter writer = new StringWriter();
        JarFile jarFile = new JarFile(sitePaths.plugins_dir.getAbsoluteFile() + File.separator + "gerrit-ci.jar");
        if (params.get("junitEnabled").toString().equals("false")) {
            params.put("junitPath", "");
        }
        IOUtils.copy(jarFile.getInputStream(jarFile.getEntry("templates" + type.getTemplate())), writer);
        String jobTemplate = writer.toString();
        writer = new StringWriter();
        IOUtils.copy(jarFile.getInputStream(jarFile.getEntry("scripts/prebuild-commands.sh")), writer);
        // We must escape special characters as this will be rendered into XML
        String prebuildScript = writer.toString().replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;");
        params.put("cleanCommands", prebuildScript);
        StringWriter xmlWriter = new StringWriter();
        Velocity.evaluate(velocityContext, xmlWriter, "", jobTemplate);
        String jobXml = xmlWriter.toString();
        jarFile.close();
        if (JenkinsProvider.jobExists(jsc, name)) {
            try {
                server.updateJob(name, jobXml, false);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Failed to update Jenkins job: %s", name), e);
            }
        } else {
            try {
                server.createJob(name, jobXml, false);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Failed to create Jenkins job: %s", name), e);
            }
        }
    }

    private String parseBranchRegex(String s) {
        if(s.startsWith("refs/heads/")) {
            s = s.replace("refs/heads/", "(?!refs/)");
        }
        s = String.format("(?!refs/meta/)%s", s);
        s = String.format("^%s$", s);
        return s;
    }
}
