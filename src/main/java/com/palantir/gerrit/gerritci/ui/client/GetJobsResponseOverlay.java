package com.palantir.gerrit.gerritci.ui.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * This class is a JNSI overlay class for the JavaScript object returned by the GET
 * /jobs/{$projectName} route.
 */
public class GetJobsResponseOverlay extends JavaScriptObject {

    protected GetJobsResponseOverlay() {}

    public final native boolean getVerifyJobEnabled()/*-{
                                                     return this.verifyJobEnabled;
                                                     }-*/;

    public final native String getVerifyBranchRegex()/*-{
                                                     return this.verifyBranchRegex;
                                                     }-*/;

    public final native boolean getPublishJobEnabled()/*-{
                                                      return this.publishJobEnabled;
                                                      }-*/;

    public final native String getPublishBranchRegex()/*-{
                                                      return this.publishBranchRegex;
                                                      }-*/;

    public final native Boolean getTimeoutEnabled()/*-{
                                                   return this.timeoutEnabled;
                                                   }-*/;

    public final native Integer getTimeoutMinutes()/*-{
                                                   return this.timeoutMinutes;
                                                   }-*/;
}