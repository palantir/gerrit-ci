package com.palantir.gerrit.gerritci.servlets;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.palantir.gerrit.gerritci.constants.JobType;
import com.palantir.gerrit.gerritci.models.JenkinsServerConfiguration;
import com.palantir.gerrit.gerritci.providers.JenkinsProvider;

@Singleton
public class JobsServlet extends HttpServlet {

    private static final long serialVersionUID = -4428173510340797397L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException,
        IOException {
        res.getWriter().write("Hello from GET!\n");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException,
        IOException {

        // TODO: Replace this with the request body
        JenkinsServerConfiguration jsc = new JenkinsServerConfiguration();
        try {
            jsc.setUri(new URI("http://localhost:8000"));
        } catch(URISyntaxException e) {}
        Map<String, String> params = ImmutableMap.of("stuff", "stuff");

        JenkinsProvider.createJob(jsc, "group_repo_verify", JobType.VERIFY, params);
        JenkinsProvider.createJob(jsc, "group_repo_publish", JobType.PUBLISH, params);

        res.setStatus(200);
        res.setContentType("test/plain");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(String.format("Created jobs for %s", "PROJECT-NAME"));
    }
}
