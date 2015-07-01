package com.palantir.gerrit.gerritci.servlets;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.io.CharStreams;
import com.google.gerrit.common.data.GerritConfig;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
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

@Singleton
public class JobsServlet extends HttpServlet {

    private static final long serialVersionUID = -4428173510340797397L;
    private ProjectControl.Factory projectControlFactory;
    private GerritConfig gerritConfig;
    private String canonicalWebUrl;

    @Inject
    public JobsServlet(final ProjectControl.Factory projectControlFactory,
        final GerritConfig gerritConfig, @CanonicalWebUrl String canonicalWebUrl) {
        this.projectControlFactory = projectControlFactory;
        this.gerritConfig = gerritConfig;
        this.canonicalWebUrl = canonicalWebUrl;
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

        // TODO: Replace this with the request body
        JenkinsServerConfiguration jsc = new JenkinsServerConfiguration();
        try {
            jsc.setUri(new URI("http://localhost:8000"));
        } catch(URISyntaxException e) {}

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
        params.put("verifyBranchRegex", requestParams.get("verifyBranchRegex").getAsString());

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
        params.put("publishBranchRegex", requestParams.get("publishBranchRegex").getAsString());

        // publishCommand
        if(!requestParams.has("publishCommand")) {
            res.setStatus(400);
            return;
        }
        params.put("publishCommand", requestParams.get("publishCommand").getAsString());

        // timeoutEnabled
        if(!requestParams.has("timeoutEnabled")) {
            res.setStatus(400);
            return;
        }
        params.put("timeoutEnabled", requestParams.get("timeoutEnabled").getAsJsonObject().get("b")
            .getAsBoolean());

        // timeoutMinutes
        if(!requestParams.has("timeoutMinutes")) {
            res.setStatus(400);
            return;
        }
        params.put("timeoutMinutes", requestParams.get("timeoutMinutes").getAsJsonObject().get("b")
            .getAsInt());

        // TODO: Replace this with the request body
        JenkinsServerConfiguration jsc = new JenkinsServerConfiguration();
        try {
            jsc.setUri(new URI("http://localhost:8000"));
        } catch(URISyntaxException e) {}

        String sshPort = gerritConfig.getSshdAddress();
        sshPort = sshPort.substring(sshPort.lastIndexOf(':') + 1);

        String host = canonicalWebUrl.replace("https://", "").replace("http://", "");
        host = host.substring(0, host.indexOf(':'));

        params.put("gerritUser", "jenkins");
        params.put("host", host);
        params.put("port", sshPort);

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
