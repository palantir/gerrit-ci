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

public class JenkinsConfig extends JavaScriptObject {
	protected JenkinsConfig() {
	}

	public final native String getJenkinsURL()/*-{
                                                         return this.jenkinsURL;
                                                         }-*/;

	public final native String getJenkinsUser()/*-{
                                                         return this.jenkinsUser;
                                                         }-*/;

	public final native String getJenkinsPassword()/*-{
                                                         return this.jenkinsPassword;
                                                         }-*/;

	public final native String getGerritUser()/*-{
                                                         return this.gerritUser;
                                                         }-*/;

	public final native String getCredentialsId()/*-{
                                                         return this.credentialsId;
                                                         }-*/;

}
