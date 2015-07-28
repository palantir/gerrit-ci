package com.palantir.gerrit.gerritci.constants;

/**
 * This class houses special constants that are used in various places throughout the plugin. In the
 * future, the constants in this class will be replaced with external configuration.
 */
public class Constants {
    /*
     * Note: The values below are configured for the test servers. When building for other
     * servers, these values will need to be changed appropriately.
     */
    public static final String JENKINS_URL = "http://localhost:8000";
    public static final String GERRIT_USER = "jenkins";
    public static final String JENKINS_USER = null;
    public static final String JENKINS_PASSWORD = null;
    public static final String CREDENTIALS_ID = "612940dd-cadf-43f0-98dc-e2f02f5e68ec";
}
