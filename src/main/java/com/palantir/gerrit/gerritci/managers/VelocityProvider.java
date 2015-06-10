package com.palantir.gerrit.gerritci.managers;

import java.io.File;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.URLResourceLoader;

public class VelocityProvider {
    private final VelocityEngine velocityEngine;

    public VelocityProvider() {
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "url");
        velocityEngine.setProperty("url.resource.loader.class", URLResourceLoader.class.getName());

        File baseDirectory = new File("plugins/gerrit-ci-0.1.0.jar");
        velocityEngine.setProperty("url.resource.loader.root",
            "jar:file://" + baseDirectory.getAbsolutePath() + "!/templates");

        velocityEngine.init();
    }

    public VelocityEngine getVelocityEngine() {
        return velocityEngine;
    }

    public VelocityContext getVelocityContext() {
        return new VelocityContext();
    }
}
