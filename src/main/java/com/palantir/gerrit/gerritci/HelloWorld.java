package com.palantir.gerrit.gerritci;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gerrit.extensions.annotations.Export;
import com.google.gerrit.server.CurrentUser;
import com.google.inject.Inject;
import com.google.inject.Provider;

@Export("/hello")
public class HelloWorld extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 4557825282440222357L;

    private CurrentUser currentUser;

    @Inject
    public HelloWorld(Provider<CurrentUser> currentUser) {
        this.currentUser = currentUser.get();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PrintWriter out = res.getWriter();
        try {
            out.write("Hello, " + currentUser.getUserName() + "!");
        } finally {
            out.close();
        }
    }
}
