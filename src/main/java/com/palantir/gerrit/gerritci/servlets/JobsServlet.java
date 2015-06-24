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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.CurrentUser;
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

@Singleton
public class JobsServlet extends HttpServlet {

    private static final long serialVersionUID = -4428173510340797397L;
    private static final Logger logger = LoggerFactory.getLogger(JobsServlet.class);
    private ProjectControl.Factory projectControlFactory;

    @Inject
    public JobsServlet(final ProjectControl.Factory projectControlFactory) {
        this.projectControlFactory = projectControlFactory;
    }

    private boolean currentUserCanAccess() {
        try {
            ProjectControl projectControl =
                this.projectControlFactory.controlFor(new NameKey("All-Projects"));
            CurrentUser user = projectControl.getCurrentUser();

            // TODO: Check if the current user has permissions for the project in question

            // If the current user is not signed in, the real user will be "ANONYMOUS"
            return !user.getRealUser().toString().equals("ANONYMOUS");
        } catch(NoSuchProjectException e) {
            logger.error("Failed API access test. Can't find All-Projects", e);
            return false;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException,
        IOException {
        if(!currentUserCanAccess()) {
            res.setStatus(401);
            return;
        }

        res.getWriter().write("Hello from GET!");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException,
        IOException {
        if(!currentUserCanAccess()) {
            res.setStatus(401);
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

        Map<String, String> params = new HashMap<String, String>();

        if(!requestParams.has("projectName")) {
            res.setStatus(400);
            return;
        }
        String projectName = requestParams.get("projectName").getAsString();
        params.put("projectName", projectName);

        // TODO: Replace this with the request body
        JenkinsServerConfiguration jsc = new JenkinsServerConfiguration();
        try {
            jsc.setUri(new URI("http://localhost:8000"));
        } catch(URISyntaxException e) {}

        String verifyJobName = String.format("%s_verify", projectName.replace('/', '_'));
        String publishJobName = String.format("%s_publish", projectName.replace('/', '_'));

        try {
            JenkinsProvider.createJob(jsc, verifyJobName, JobType.VERIFY, params);
            JenkinsProvider.createJob(jsc, publishJobName, JobType.PUBLISH, params);
        } catch(IllegalArgumentException e) {
            logger.error("Jobs already exist on Jenkins", e);
            res.setStatus(500);
            return;
        }

        res.setStatus(200);
        res.setContentType("text/plain");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(String.format("Created jobs!"));
    }
}
