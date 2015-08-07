package com.palantir.gerrit.gerritci.servlets;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.palantir.gerrit.gerritci.constants.JobType;
import com.palantir.gerrit.gerritci.models.JenkinsServerConfiguration;
import com.palantir.gerrit.gerritci.providers.JenkinsProvider;
import com.palantir.gerrit.gerritci.util.JenkinsJobParser;
import org.apache.commons.io.IOUtils;

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

        int responseCode = getResponseCode(projectName);
        if(responseCode != 200) {
            res.setStatus(responseCode);
            return;
        }
        FileBasedConfig cfg =
                new FileBasedConfig(new File(sitePaths.etc_dir, "gerrit-ci.config"), FS.DETECTED);
        try {
            cfg.load();
        } catch(ConfigInvalidException e) {
            logger.info("Error loading config file after get request:", e);
        }

        String jenkinsUrlString = cfg.getString("Settings", "Jenkins", "jenkinsURL");
        String jenkinsUserString = cfg.getString("Settings", "Jenkins", "jenkinsUser");
        String jenkinsPasswordString = cfg.getString("Settings", "Jenkins", "jenkinsPassword");

        JenkinsServerConfiguration jsc = new JenkinsServerConfiguration();

        try {
            jsc.setUri(new URI(jenkinsUrlString));
        } catch(URISyntaxException e) {
        }
        jsc.setUsername(jenkinsUserString);
        jsc.setPassword(jenkinsPasswordString);

        JsonObject params = JenkinsJobParser.parseJenkinsJob(projectName, jsc);

        res.setStatus(200);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(params.toString());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException,
        IOException {
        String encodedProjectName =
            req.getRequestURI().substring(req.getRequestURI().lastIndexOf('/') + 1);
        String projectName = encodedProjectName.replace("%2F", "/");

        int responseCode = getResponseCode(projectName);
        if(responseCode != 200) {
            res.setStatus(responseCode);
            return;
        }

        /*
         * The actual parameters we send are encoded into a JSON object such that they are contained
         * in an object under the entry "f". The other top-level keys seem to be useless. In
         * addition, each key in the parameters object has ":" prefixed to whatever it is the key is
         * actually named. Thus, by stripping the first character away from each key, we arrive at a
         * sane JSONObject of request parameters.
         * Example: {"b": [], "f": {":projectName": "name", ":verifyBranchRegex": * ".*"}}
         */
        JsonObject requestBody =
            (JsonObject) (new JsonParser()).parse(CharStreams.toString(req.getReader()));
        requestBody = requestBody.get("f").getAsJsonObject();

        JsonObject requestParams = new JsonObject();
        for(Entry<String, JsonElement> e: requestBody.entrySet()) {
            requestParams.add(e.getKey().substring(1), e.getValue());
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("projectName", projectName);

        JarFile jarFile = new JarFile(sitePaths.plugins_dir.getAbsoluteFile() + File.separator +
                "gerrit-ci.jar");
        StringWriter writer = new StringWriter();
        IOUtils.copy(jarFile.getInputStream(jarFile.getEntry("scripts/prebuild-commands.sh")),
                writer);

        // We must escape special characters as this will be rendered into XML
        String prebuildScript =
                writer.toString().replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;");
        params.put("cleanCommands", prebuildScript);

        // verifyJobEnabled
        if(!requestParams.has("verifyJobEnabled")) {
            res.setStatus(400);
            return;
        }
        boolean verifyJobEnabled =
            requestParams.get("verifyJobEnabled").getAsJsonObject().get("b").getAsBoolean();

        // verifyBranchRegex
        if(!requestParams.has("verifyBranchRegex")) {
            res.setStatus(400);
            return;
        }

        String verifyBranchRegex = requestParams.get("verifyBranchRegex").getAsString();
        if(verifyBranchRegex.startsWith("refs/heads/")) {
            verifyBranchRegex = verifyBranchRegex.replace("refs/heads/", "(?!refs/)");
        }
        verifyBranchRegex = String.format("(?!refs/meta/)%s", verifyBranchRegex);
        verifyBranchRegex = String.format("^%s$", verifyBranchRegex);
        params.put("verifyBranchRegex", verifyBranchRegex);

        // verifyCommand
        if(!requestParams.has("verifyCommand")) {
            res.setStatus(400);
            return;
        }
        params.put("verifyCommand", requestParams.get("verifyCommand").getAsString());

        // publishJobEnabled
        if(!requestParams.has("publishJobEnabled")) {
            res.setStatus(400);
            return;
        }
        boolean publishJobEnabled =
            requestParams.get("publishJobEnabled").getAsJsonObject().get("b").getAsBoolean();

        // publishBranchRegex
        if(!requestParams.has("publishBranchRegex")) {
            res.setStatus(400);
            return;
        }
        String publishBranchRegex = requestParams.get("publishBranchRegex").getAsString();
        if(!publishBranchRegex.startsWith("refs/heads/")) {
            res.setStatus(400);
            return;
        }
        publishBranchRegex = publishBranchRegex.replace("refs/heads/", "(?!refs/)");
        publishBranchRegex = String.format("^%s$", publishBranchRegex);
        params.put("publishBranchRegex", publishBranchRegex);

        // publishCommand
        if(!requestParams.has("publishCommand")) {
            res.setStatus(400);
            return;
        }
        params.put("publishCommand", requestParams.get("publishCommand").getAsString());

        // timeoutMinutes
        if(!requestParams.has("timeoutMinutes")) {
            res.setStatus(400);
            return;
        }
        params.put("timeoutMinutes",
                requestParams.get("timeoutMinutes").getAsJsonObject().get("b").getAsInt());


        FileBasedConfig cfg =
                new FileBasedConfig(new File(sitePaths.etc_dir, "gerrit-ci.config"), FS.DETECTED);
        try {
            cfg.load();
        } catch(ConfigInvalidException e) {
            logger.info("Error loading config file after get request:", e);
        }

        String jenkinsUrlString = cfg.getString("Settings", "Jenkins", "jenkinsURL");
        String jenkinsUserString = cfg.getString("Settings", "Jenkins", "jenkinsUser");
        String jenkinsPasswordString = cfg.getString("Settings", "Jenkins", "jenkinsPassword");

        // Add junit post build action
        if(!requestParams.has("junitEnabled")) {
            res.setStatus(400);
            return;
        }

        params.put("junitEnabled", requestParams.get("junitEnabled").getAsJsonObject().get("b")
           .getAsBoolean());

        // Path to publish junit test results
        if(!requestParams.has("junitPath")) {
            res.setStatus(400);
            return;
        }
        params.put("junitPath", requestParams.get("junitPath").getAsString());

        // TODO: Replace this with the request body
        JenkinsServerConfiguration jsc = new JenkinsServerConfiguration();
        try {
            jsc.setUri(new URI(jenkinsUrlString));
        } catch(URISyntaxException e) {
        }
        jsc.setUsername(jenkinsUserString);
        jsc.setPassword(jenkinsPasswordString);

        String sshPort = gerritConfig.getSshdAddress();
        sshPort = sshPort.substring(sshPort.lastIndexOf(':') + 1);

        String host = canonicalWebUrl.replace("https://", "").replace("http://", "");
        if(host.contains(":")) {
            host = host.substring(0, host.indexOf(':'));
        }
        if(host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }

        params.put("gerritUser", cfg.getString("Settings", "Jenkins", "gerritUser"));
        params.put("host", host);
        params.put("port", sshPort);
        params.put("credentialsId", cfg.getString("Settings", "Jenkins", "credentialsId"));

        String verifyJobName = JobType.VERIFY.getJobName(projectName);
        String publishJobName = JobType.PUBLISH.getJobName(projectName);

        if(verifyJobEnabled) {
            JenkinsProvider.createOrUpdateJob(jsc, verifyJobName, JobType.VERIFY, params);
        } else {
            JenkinsProvider.deleteJob(jsc, verifyJobName);
        }

        if(publishJobEnabled) {
            JenkinsProvider.createOrUpdateJob(jsc, publishJobName, JobType.PUBLISH, params);
        } else {
            JenkinsProvider.deleteJob(jsc, publishJobName);
        }

        res.setStatus(200);
        res.setContentType("text/plain");
    }
}
