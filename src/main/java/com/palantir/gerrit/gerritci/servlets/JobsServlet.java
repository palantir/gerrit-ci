package com.palantir.gerrit.gerritci.servlets;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
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

        // TODO: Replace this with the request body
        JenkinsServerConfiguration jsc = new JenkinsServerConfiguration();
        try {
            jsc.setUri(new URI("http://localhost:8000"));
        } catch(URISyntaxException e) {}
        Map<String, String> params = ImmutableMap.of("stuff", "stuff");

        try {
            JenkinsProvider.createJob(jsc, "group_repo_verify", JobType.VERIFY, params);
            JenkinsProvider.createJob(jsc, "group_repo_publish", JobType.PUBLISH, params);
        } catch(IllegalArgumentException e) {
            logger.error("Jobs already exist on Jenkins", e);
            res.setStatus(500);
            return;
        }

        res.setStatus(200);
        res.setContentType("test/plain");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(String.format("Created jobs!"));
    }
}
