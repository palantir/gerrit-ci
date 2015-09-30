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
// Copyright (C) 2014 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.palantir.gerrit.gerritci.ui.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ConfigurationScreen extends VerticalPanel {

	static class Factory implements Screen.EntryPoint {
		@Override
		public void onLoad(Screen screen) {
			screen.setPageTitle("Gerrit-CI Admin Settings");
			screen.show(new ConfigurationScreen());
		}
	}

	private final TextBox jenkinsURL;
	private final TextBox jenkinsUser;
	private final TextBox jenkinsPassword;
	private final TextBox gerritUser;
	private final TextBox credentialsId;

	ConfigurationScreen() {
		setStyleName("gerrit-ci");

		jenkinsURL = new TextBox();
		jenkinsUser = new TextBox();
		jenkinsPassword = new TextBox();
		gerritUser = new TextBox();
		credentialsId = new TextBox();

		// initialize with defaults
		jenkinsURL.setText("http://localhost:8000");
		jenkinsUser.setText(null);
		jenkinsPassword.setText(null);
		gerritUser.setText("jenkins");
		credentialsId.setText("612940dd-cadf-43f0-98dc-e2f02f5e68ec");

		// These settings will be the same for all Jenkins servers
		HeadingElement gerritSettings = Document.get().createHElement(2);
		gerritSettings.setInnerText("Gerrit Settings for Jenkins");

		ParagraphElement gerritSettingsDescription = Document.get().createPElement();
		gerritSettingsDescription.setClassName("description");
		gerritSettingsDescription
		.setInnerText("The configurations below are similar for all Jenkins instances. " +
				"They describe how Jenkins will connect to Gerrit. For example: 'Gerrit User' " +
				"is Jenkins's username in Gerrit.");

		ParagraphElement gerritUserLabel = Document.get().createPElement();
		gerritUserLabel.setInnerText("Gerrit User");
		gerritUser.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(KeyPressEvent event) {
				event.stopPropagation();
			}
		});

		// These settings will be specific for a Jenkins server instance
		// when multiple Jenkins servers are supported by gerrit-ci
		HeadingElement serverSettings = Document.get().createHElement(2);
		serverSettings.setInnerText("Jenkins Settings for Gerrit");
		ParagraphElement serverSettingsDescription = Document.get().createPElement();
		serverSettingsDescription.setClassName("description");
		serverSettingsDescription
		.setInnerText("The configurations below are specific to each instance of Jenkins." +
				"They describe how Gerrit will connect to a specific instance of Jenkins.");

		// create labels
		ParagraphElement jenkinsURLLabel = Document.get().createPElement();
		jenkinsURLLabel.setInnerText("Jenkins Url");
		jenkinsURL.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(KeyPressEvent event) {
				event.stopPropagation();
			}
		});

		ParagraphElement jenkinsUserLabel = Document.get().createPElement();
		jenkinsUserLabel.setInnerText("Jenkins User");
		jenkinsUser.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(KeyPressEvent event) {
				event.stopPropagation();
			}
		});

		ParagraphElement jenkinsPasswordLabel = Document.get().createPElement();
		jenkinsPasswordLabel.setInnerText("Jenkins Password");
		add(HTML.wrap(jenkinsURLLabel));
		jenkinsPassword.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(KeyPressEvent event) {
				event.stopPropagation();
			}
		});


		ParagraphElement crendentialsIdLabel = Document.get().createPElement();
		crendentialsIdLabel.setInnerText("Credentials Id");
		credentialsId.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(KeyPressEvent event) {
				event.stopPropagation();
			}
		});

		// add to widgets screen
		add(HTML.wrap(gerritSettings));
		add(HTML.wrap(gerritSettingsDescription));
		add(HTML.wrap(gerritUserLabel));
		add(gerritUser);
		add(HTML.wrap(serverSettings));
		add(HTML.wrap(serverSettingsDescription));
		add(HTML.wrap(jenkinsURLLabel));
		add(jenkinsURL);
		add(HTML.wrap(jenkinsUserLabel));
		add(jenkinsUser);
		add(HTML.wrap(jenkinsPasswordLabel));
		add(jenkinsPassword);
		add(HTML.wrap(crendentialsIdLabel));
		add(credentialsId);

		// retrieve values from gerrit-ci.config stored in the plugins config
		// file on gerrit
		new RestApi("plugins").id("gerrit-ci").view("settings")
		.get(new AsyncCallback<JavaScriptObject>() {
			@Override
			public void onSuccess(JavaScriptObject jenkinsConfig) {
				JenkinsConfig config = (JenkinsConfig) jenkinsConfig;
				String jenkinsURLString = config.getJenkinsURL();
				String jenkinsUserString = config.getJenkinsUser();
				String jenkinsPasswordString = config
						.getJenkinsPassword();
				String gerritUserString = config.getGerritUser();
				String credentialIdString = config.getCredentialsId();
				if (jenkinsURLString != null) {
					jenkinsURL.setText(jenkinsURLString);
				}
				if (jenkinsUserString != null) {
					jenkinsUser.setText(jenkinsUserString);
				}
				if (jenkinsPasswordString != null) {
					jenkinsPassword.setText(jenkinsPasswordString);
				}
				if (gerritUserString != null) {
					gerritUser.setText(gerritUserString);
				}
				if (credentialIdString != null) {
					credentialsId.setText(credentialIdString);
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				// never invoked
			}
		});

		Button save = new Button("Save");
		save.setEnabled(true);
		save.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				save();
			}
		});
		add(save);
	}

	private void save() {
		Map<String, String> params = new HashMap<String, String>();

		params.put("jenkinsURL", jenkinsURL.getText());
		params.put("jenkinsUser", jenkinsUser.getText());
		params.put("jenkinsPassword", jenkinsPassword.getText());
		params.put("gerritUser", gerritUser.getText());
		params.put("credentialsId", credentialsId.getText());

		// update gerrit-ci.config with new values
		JavaScriptObject input = (JavaScriptObject) params;
		new RestApi("plugins").id("gerrit-ci").view("settings")
		.put(input, new AsyncCallback<JavaScriptObject>() {
			@Override
			public void onSuccess(JavaScriptObject configs) {
						alertWidget("Gerrit Server Response",
								"Gerrit-ci has been updated successfully with your settings")
						.center();
			}

			@Override
			public void onFailure(Throwable caught) {
				// never invoked
			}
		});
	}

	/**
	 * Creates a DialogBox object with the specified header and content and
	 * returns it to be displayed.
	 *
	 * @param header
	 *            Title for the alert dialog
	 * @param content
	 *            Inner message content to display
	 * @return A DialogBox object configured with the specified strings
	 */
	public static DialogBox alertWidget(final String header,
			final String content) {
		final DialogBox dialogBox = new DialogBox();
		final VerticalPanel verticalPanel = new VerticalPanel();

		final Label emptyLabel = new Label("");
		emptyLabel.setSize("auto", "25px");

		dialogBox.setText(header);

		verticalPanel.add(emptyLabel);
		verticalPanel.add(new Label(content));
		verticalPanel.add(emptyLabel);

		final Button buttonClose = new Button("Close", new ClickHandler() {

			@Override
			public void onClick(final ClickEvent event) {
				dialogBox.hide();
			}
		});
		buttonClose.setWidth("100px");
		verticalPanel.add(buttonClose);
		verticalPanel.setCellHorizontalAlignment(buttonClose,
				HasAlignment.ALIGN_CENTER);

		dialogBox.add(verticalPanel);
		return dialogBox;
	}
}
