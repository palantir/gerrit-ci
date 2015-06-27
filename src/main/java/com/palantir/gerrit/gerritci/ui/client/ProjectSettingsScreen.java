package com.palantir.gerrit.gerritci.ui.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.PluginEntryPoint;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * This class shows the Gerrit-CI settings screen for a particular project to users with the proper
 * permissions.
 */
public class ProjectSettingsScreen extends PluginEntryPoint {

    /**
     * The name of the currently-selected project
     */
    private String projectName;

    /**
     * CheckBox that will enable or disable verify jobs
     */
    private CheckBox verifyJobEnabled;

    /**
     * TextBox that contains the regex describing which branches to run verify jobs on
     */
    private TextBox verifyBranchRegex;

    /**
     * CheckBox that will enable or disable publish jobs
     */
    private CheckBox publishJobEnabled;

    /**
     * TextBox that contains the regex describing which branches to run publish jobs on
     */
    private TextBox publishBranchRegex;

    /**
     * CheckBox that will enable or disable timeouts for jobs
     */
    private CheckBox timeoutEnabled;

    /**
     * TextBox that contains the number of minutes to wait for the timeout
     */
    private TextBox timeoutMinutes;

    /**
     * Button that saves the project configuration and creates the jobs
     */
    private Button saveButton;

    @Override
    public void onPluginLoad() {

        /*
         * The regex will match all strings following "projects/". This is necessary because project
         * names can contain slashes.
         */
        Plugin.get().screenRegex("projects/.+", new Screen.EntryPoint() {

            @Override
            public void onLoad(Screen screen) {
                /*
                 * History.getToken() returns the current page's URL, which we can parse to get the
                 * current project's name.
                 */
                final String encodedProjectName =
                    History.getToken().replace("/x/gerrit-ci/projects/", "");
                projectName = encodedProjectName.replace("%2F", "/");

                // Instantiate widgets
                VerticalPanel verticalPanel = new VerticalPanel();

                verifyJobEnabled = new CheckBox("Disabled");
                verifyBranchRegex = new TextBox();
                publishJobEnabled = new CheckBox("Disabled");
                publishBranchRegex = new TextBox();
                timeoutEnabled = new CheckBox("Disabled");
                timeoutMinutes = new TextBox();
                saveButton = new Button("Save");

                // header
                HeadingElement header = Document.get().createHElement(1);
                header.setInnerText("Gerrit-CI Settings for Project: " + projectName);
                verticalPanel.add(HTML.wrap(header));

                // verifyHeader
                HeadingElement verifyHeader = Document.get().createHElement(2);
                verifyHeader.setInnerText("Verify Job");
                verticalPanel.add(HTML.wrap(verifyHeader));

                // verifyJobEnabled
                verifyJobEnabled.setValue(false);
                verifyJobEnabled.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        boolean isEnabled = verifyJobEnabled.getValue();
                        verifyJobEnabled.setText(isEnabled ? "Enabled" : "Disabled");
                        updateWidgetEnablity();
                    }
                });
                verticalPanel.add(verifyJobEnabled);

                // verifyBranchRegex
                verifyBranchRegex.setText(".*");
                verticalPanel.add(verifyBranchRegex);

                // publishHeader
                HeadingElement publishHeader = Document.get().createHElement(2);
                publishHeader.setInnerText("Publish Job");
                verticalPanel.add(HTML.wrap(publishHeader));

                // publishJobEnabled
                publishJobEnabled.setValue(false);
                publishJobEnabled.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        boolean isEnabled = publishJobEnabled.getValue();
                        publishJobEnabled.setText(isEnabled ? "Enabled" : "Disabled");
                        updateWidgetEnablity();
                    }
                });
                verticalPanel.add(publishJobEnabled);

                // publishBranchRegex
                publishBranchRegex.setText("refs/heads/(develop|master)");
                verticalPanel.add(publishBranchRegex);

                // generalHeader
                HeadingElement generalHeader = Document.get().createHElement(2);
                generalHeader.setInnerText("General Settings");
                verticalPanel.add(HTML.wrap(generalHeader));

                // timeoutHeader
                HeadingElement timeoutHeader = Document.get().createHElement(3);
                timeoutHeader.setInnerText("Timeouts");
                verticalPanel.add(HTML.wrap(timeoutHeader));

                // timeoutEnabled
                timeoutEnabled.setValue(false);
                timeoutEnabled.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        boolean isEnabled = timeoutEnabled.getValue();
                        timeoutEnabled.setText(isEnabled ? "Enabled" : "Disabled");
                        updateWidgetEnablity();
                    }
                });
                verticalPanel.add(timeoutEnabled);

                // timeoutMinutes
                timeoutMinutes.setText("15");
                verticalPanel.add(timeoutMinutes);

                // saveButton
                saveButton.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        Map<String, Object> params = new HashMap<String, Object>();
                        params.put("projectName", projectName);
                        params.put("verifyJobEnabled", verifyJobEnabled.getValue());
                        params.put("verifyBranchRegex", verifyBranchRegex.getText());
                        params.put("publishJobEnabled", publishJobEnabled.getValue());
                        params.put("publishBranchRegex", publishBranchRegex.getText());
                        params.put("timeoutEnabled", timeoutEnabled.getValue());
                        params.put("timeoutMinutes", Integer.valueOf(timeoutMinutes.getText()));

                        new RestApi("plugins").id("gerrit-ci").view("jobs")
                            .view(encodedProjectName)
                            .put((JavaScriptObject) params, new AsyncCallback<JavaScriptObject>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    // TODO: Handle this situation
                                }

                                @Override
                                public void onSuccess(JavaScriptObject result) {
                                    // TODO: Handle this situation
                                }
                            });
                    }
                });
                verticalPanel.add(saveButton);

                updateWidgetEnablity();

                screen.add(verticalPanel);
                screen.show();

                new RestApi("plugins").id("gerrit-ci").view("jobs").view(encodedProjectName)
                    .get(new AsyncCallback<JavaScriptObject>() {

                        @Override
                        public void onFailure(Throwable caught) {
                            // TODO: Handle this situation
                        }

                        @Override
                        public void onSuccess(JavaScriptObject result) {
                            GetJobsResponseOverlay config = (GetJobsResponseOverlay) result;

                            verifyJobEnabled.setValue(config.getVerifyJobEnabled());
                            verifyJobEnabled.setText(verifyJobEnabled.getValue() ? "Enabled"
                                : "Disabled");

                            publishJobEnabled.setValue(config.getPublishJobEnabled());
                            publishJobEnabled.setText(publishJobEnabled.getValue() ? "Enabled"
                                : "Disabled");

                            timeoutEnabled.setValue(config.getTimeoutEnabled());
                            timeoutEnabled.setText(timeoutEnabled.getValue() ? "Enabled"
                                : "Disabled");

                            String verifyBranchRegexString = config.getVerifyBranchRegex();
                            String publishBranchRegexString = config.getPublishBranchRegex();
                            Integer timeoutMinutesInteger = config.getTimeoutMinutes();

                            if(verifyBranchRegexString != null) {
                                verifyBranchRegex.setText(verifyBranchRegexString);
                            }

                            if(publishBranchRegexString != null) {
                                publishBranchRegex.setText(publishBranchRegexString);
                            }

                            if(timeoutMinutesInteger != null) {
                                timeoutMinutes.setText(String.valueOf(timeoutMinutesInteger));
                            }

                            updateWidgetEnablity();
                        }
                    });
            }
        });
    }

    /**
     * To be called after updating the enability (yes, I invented a word to describe the state of
     * being enabled/disabled) of a CheckBox. This method will update the enability of other
     * widgets based on the change.
     */
    private void updateWidgetEnablity() {
        verifyBranchRegex.setEnabled(verifyJobEnabled.getValue());
        publishBranchRegex.setEnabled(publishJobEnabled.getValue());
        timeoutEnabled.setEnabled(verifyJobEnabled.getValue() || publishJobEnabled.getValue());
        timeoutMinutes.setEnabled(timeoutEnabled.getValue());
    }
}
