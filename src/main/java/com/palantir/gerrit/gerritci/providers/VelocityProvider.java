package com.palantir.gerrit.gerritci.providers;

import java.io.File;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.URLResourceLoader;

/**
 * This class provides a set of methods that control the use of the Velocity library for XML
 * creation using template files.
 */
public class VelocityProvider {

    /**
     * Class which is used to create the XML files. Final so that we only have one VelocityEngine
     * instance per VelocityProvider instance.
     */
    private final VelocityEngine velocityEngine;

    /**
     * Instantiates and initializes the velocityEngine field. Sets the ResourceLoader as
     * URLResourceLoader so that we can load the template files directly from a JAR file on the
     * filesystem.
     */
    public VelocityProvider() {
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "url");
        velocityEngine.setProperty("url.resource.loader.class", URLResourceLoader.class.getName());

        // The current working directory is the root of the Gerrit server
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
