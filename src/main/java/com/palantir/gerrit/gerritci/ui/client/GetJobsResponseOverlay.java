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

    public final native String getPublishCommand()/*-{
                                                  return this.publishCommand;
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
