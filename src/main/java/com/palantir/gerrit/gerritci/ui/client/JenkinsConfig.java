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
