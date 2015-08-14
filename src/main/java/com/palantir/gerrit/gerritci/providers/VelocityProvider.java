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
package com.palantir.gerrit.gerritci.providers;

import java.io.File;
import java.util.Map;

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
        File baseDirectory = new File("plugins/gerrit-ci.jar");
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

    public VelocityContext getVelocityContext(Map<String, Object> params) {
        return new VelocityContext(params);
    }
}
