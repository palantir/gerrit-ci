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

    public final native String getVerifyCommand()/*-{
                                                 return this.verifyCommand;
                                                 }-*/;

    public final native boolean getPublishJobEnabled()/*-{
                                                      return this.publishJobEnabled;
                                                      }-*/;

    public final native String getPublishBranchRegex()/*-{
                                                      return this.publishBranchRegex;
                                                      }-*/;

    public final native boolean getCronJobEnabled()/*-{
                                                      return this.cronJobEnabled;
                                                      }-*/;

    public final native String getPublishCommand()/*-{
                                                  return this.publishCommand;
                                                  }-*/;

    public final native String getCronCommand()/*-{
                                           return this.cronCommand;
                                           }-*/;

    public final native String getCronJob()/*-{
                                           return this.cronJob;
                                           }-*/;

    public final native Integer getTimeoutMinutes()/*-{
                                                   return this.timeoutMinutes;
                                                   }-*/;
    public final native boolean getJunitEnabled()/*-{
                                                   return this.junitEnabled;
                                                   }-*/;
    public final native String getJunitPath()/*-{
                                                  return this.junitPath;
                                                  }-*/;
    public final native String getErrorMsg()/*-{
                                                return this.error;
                                              }-*/;

}
