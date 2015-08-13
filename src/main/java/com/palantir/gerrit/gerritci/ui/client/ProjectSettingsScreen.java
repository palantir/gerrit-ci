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

import java.util.HashMap;
import java.util.Map;

import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.PluginEntryPoint;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ToggleButton;
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
     * TextBox that contains the name of the command to run for verify jobs
     */
    private TextBox verifyCommand;

    /**
     * CheckBox that will enable or disable publish jobs
     */
    private CheckBox publishJobEnabled;

    /**
     * TextBox that contains the regex describing which branches to run publish jobs on
     */
    private TextBox publishBranchRegex;

    /**
     * TextBox that contains the name of the command to run for cron jobs
     */
    private TextBox cronCommand;

    /**
     * CheckBox that will enable or disable cron jobs
     */
    private CheckBox cronJobEnabled;

    /**
     * TextBox that contains the schedule to run builds
     */
    private TextBox cronJob;

    /**
     * Toggle button for cron syntax
     */
    private ToggleButton toggle;

    /**
     * TextBox that contains the name of the command to run for publish jobs
     */
    private TextBox publishCommand;

    /**
     * TextBox that contains the number of minutes to wait for the timeout
     */
    private TextBox timeoutMinutes;

    /**
     * TextBox that contains the JUnit test directory
     */
    private TextBox junitPath;

    /**
     * CheckBox that will enable or disable JUnit test publishing
     */
    private CheckBox junitEnabled;

    /**
     * Button that saves the project configuration and creates the jobs
     */
    private Button saveButton;

    public static final Resources RESOURCES = GWT.create(Resources.class);

    @Override
    public void onPluginLoad() {

        //Screen to configure settings to the gerrit-ci Plugin
        Plugin.get().screen("settings", new ConfigurationScreen.Factory());
        /*
         * The regex will match all strings following "projects/". This is necessary because project
         * names can contain slashes.
         */
        Plugin.get().screenRegex("projects/.+", new Screen.EntryPoint() {

            @Override
            public void onLoad(Screen screen) {
                final String encodedProjectName =
                    Window.Location.getHash().replace("#/x/gerrit-ci/projects/", "");
                projectName = encodedProjectName.replace("%2F", "/");

                // Instantiate widgets
                VerticalPanel verticalPanel = new VerticalPanel();
                verticalPanel.addStyleName("gerrit-ci");

                verifyJobEnabled = new CheckBox("Enable Verify Jobs");
                verifyBranchRegex = new TextBox();
                verifyCommand = new TextBox();
                publishJobEnabled = new CheckBox("Enable Publish Jobs");
                publishBranchRegex = new TextBox();
                publishCommand = new TextBox();
                cronJobEnabled = new CheckBox("Enable Time-Triggered Jobs");
                cronCommand = new TextBox();
                cronJob = new TextBox();
                toggle = new ToggleButton(new Image(RESOURCES.info()));
                toggle.setEnabled(true);
                toggle.setPixelSize(20, 20);
                junitEnabled = new CheckBox("Publish JUnit test result report");
                junitPath = new TextBox();
                timeoutMinutes = new TextBox();
                saveButton = new Button("Save & Update");

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
                verifyJobEnabled.setEnabled(false);
                verifyJobEnabled.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        updateWidgetEnablity();
                    }
                });
                verticalPanel.add(verifyJobEnabled);
                ParagraphElement verifyJobEnabledDescription = Document.get().createPElement();
                verifyJobEnabledDescription.setClassName("description");
                verifyJobEnabledDescription
                    .setInnerText("Check this checkbox to enable verify jobs for your project");
                verticalPanel.add(HTML.wrap(verifyJobEnabledDescription));

                // verifyBranchRegex
                ParagraphElement verifyBranchRegexLabel = Document.get().createPElement();
                verifyBranchRegexLabel.setInnerText("Regex for branches to verify");
                verifyBranchRegexLabel.setClassName("label");
                verticalPanel.add(HTML.wrap(verifyBranchRegexLabel));
                verifyBranchRegex.setText(".*");
                verifyBranchRegex.addKeyPressHandler(new KeyPressHandler() {
                    @Override
                    public void onKeyPress(KeyPressEvent event) {
                        event.stopPropagation();
                    }
                });
                verticalPanel.add(verifyBranchRegex);
                ParagraphElement verifyBranchRegexDescription = Document.get().createPElement();
                verifyBranchRegexDescription.setClassName("description");
                verifyBranchRegexDescription
                    .setInnerText("Branches matching this regex will have the verify job command run on them by Jenkins");
                verticalPanel.add(HTML.wrap(verifyBranchRegexDescription));

                // verifyCommand
                ParagraphElement verifyCommandLabel = Document.get().createPElement();
                verifyCommandLabel.setInnerText("Command to run for a verify job");
                verifyCommandLabel.setClassName("label");
                verticalPanel.add(HTML.wrap(verifyCommandLabel));
                verifyCommand.setText("./scripts/verify.sh");
                verifyCommand.addKeyPressHandler(new KeyPressHandler() {
                    @Override
                    public void onKeyPress(KeyPressEvent event) {
                        event.stopPropagation();
                    }
                });
                verticalPanel.add(verifyCommand);
                ParagraphElement verifyCommandDescription = Document.get().createPElement();
                verifyCommandDescription.setClassName("description");
                verifyCommandDescription
                    .setInnerText("Simple command or a shell script to run for verify jobs");
                verticalPanel.add(HTML.wrap(verifyCommandDescription));

                // publishHeader
                HeadingElement publishHeader = Document.get().createHElement(2);
                publishHeader.setInnerText("Publish Job");
                verticalPanel.add(HTML.wrap(publishHeader));

                // publishJobEnabled
                publishJobEnabled.setValue(false);
                publishJobEnabled.setEnabled(false);
                publishJobEnabled.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        updateWidgetEnablity();
                    }
                });
                verticalPanel.add(publishJobEnabled);
                ParagraphElement publishJobEnabledDescription = Document.get().createPElement();
                publishJobEnabledDescription.setClassName("description");
                publishJobEnabledDescription
                    .setInnerText("Check this checkbox to enable publish jobs for your project");
                verticalPanel.add(HTML.wrap(publishJobEnabledDescription));

                // publishBranchRegex
                ParagraphElement publishBranchRegexLabel = Document.get().createPElement();
                publishBranchRegexLabel.setInnerText("Regex for branches to publish");
                publishBranchRegexLabel.setClassName("label");
                verticalPanel.add(HTML.wrap(publishBranchRegexLabel));
                publishBranchRegex.setText("refs/heads/(develop|master)");
                publishBranchRegex.addKeyPressHandler(new KeyPressHandler() {
                    @Override
                    public void onKeyPress(KeyPressEvent event) {
                        event.stopPropagation();
                    }
                });
                verticalPanel.add(publishBranchRegex);
                ParagraphElement publishBranchRegexDescription = Document.get().createPElement();
                publishBranchRegexDescription.setClassName("description");
                publishBranchRegexDescription
                    .setInnerHTML("Branches matching this regex will have the publish job command" +
                            " run on them by Jenkins.<br/>This regex must begin with \"refs/heads/\"");
                verticalPanel.add(HTML.wrap(publishBranchRegexDescription));

                // publishCommand
                ParagraphElement publishCommandLabel = Document.get().createPElement();
                publishCommandLabel.setInnerText("Command to run for a publish job");
                publishCommandLabel.setClassName("label");
                verticalPanel.add(HTML.wrap(publishCommandLabel));
                publishCommand.setText("./scripts/publish.sh");
                publishCommand.addKeyPressHandler(new KeyPressHandler() {
                    @Override
                    public void onKeyPress(KeyPressEvent event) {
                        event.stopPropagation();
                    }
                });
                verticalPanel.add(publishCommand);
                ParagraphElement publishCommandDescription = Document.get().createPElement();
                publishCommandDescription.setClassName("description");
                publishCommandDescription
                    .setInnerText("Simple command or a shell script to run for publish jobs");
                verticalPanel.add(HTML.wrap(publishCommandDescription));

                // cronJobHeader
                HeadingElement cronHeader = Document.get().createHElement(2);
                cronHeader.setInnerText("Time-Triggered Job (Cron Job)");
                verticalPanel.add(HTML.wrap(cronHeader));

                // cronJobEnabled
                cronJobEnabled.setValue(false);
                cronJobEnabled.setEnabled(false);
                cronJobEnabled.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        updateWidgetEnablity();
                    }
                });
                verticalPanel.add(cronJobEnabled);
                ParagraphElement cronJobEnabledDescription = Document.get().createPElement();
                cronJobEnabledDescription.setClassName("description");
                cronJobEnabledDescription
                    .setInnerText("Check this checkbox to enable time-triggered jobs for your project");
                verticalPanel.add(HTML.wrap(cronJobEnabledDescription));

                // cronCommand
                ParagraphElement cronCommandLabel = Document.get().createPElement();
                cronCommandLabel.setInnerText("Command to run for time-triggered job");
                cronCommandLabel.setClassName("label");
                verticalPanel.add(HTML.wrap(cronCommandLabel));
                cronCommand.setText("./scripts/cron.sh");
                cronCommand.addKeyPressHandler(new KeyPressHandler() {
                    @Override
                    public void onKeyPress(KeyPressEvent event) {
                        event.stopPropagation();
                    }
                });
                verticalPanel.add(cronCommand);
                ParagraphElement cronCommandDescription = Document.get().createPElement();
                cronCommandDescription.setClassName("description");
                cronCommandDescription
                    .setInnerText("Simple command or a shell script to run for time-triggered jobs");
                verticalPanel.add(HTML.wrap(cronCommandDescription));

                final HorizontalPanel hp = new HorizontalPanel();
                hp.setSpacing(10);
                ParagraphElement cronJobLabel = Document.get().createPElement();
                cronJobLabel.setInnerText("Build Schedule");
                cronJobLabel.setClassName("label");
                verticalPanel.add(HTML.wrap(cronJobLabel));
                cronJob.setText("");
                cronJob.addKeyPressHandler(new KeyPressHandler() {
                    @Override
                    public void onKeyPress(KeyPressEvent event) {
                        event.stopPropagation();
                    }
                });
                hp.add(cronJob);
                final HTMLPanel cronJobDescription = new HTMLPanel(RESOURCES.cron().getText());
                toggle.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                   if (toggle.isDown()) {
                        hp.add(cronJobDescription);
                   }
                   else{
                       hp.remove(cronJobDescription);
                   }
                    }
                  });
                hp.add(toggle);
                verticalPanel.add(hp);
                // generalHeader
                HeadingElement generalHeader = Document.get().createHElement(2);
                generalHeader.setInnerText("General Settings");
                verticalPanel.add(HTML.wrap(generalHeader));

                // timeoutMinutes
                ParagraphElement timeoutMinutesLabel = Document.get().createPElement();
                timeoutMinutesLabel.setInnerText("Timeout minutes");
                timeoutMinutesLabel.setClassName("label");
                verticalPanel.add(HTML.wrap(timeoutMinutesLabel));
                timeoutMinutes.setText("30");
                timeoutMinutes.addKeyPressHandler(new KeyPressHandler() {
                    @Override
                    public void onKeyPress(KeyPressEvent event) {
                        event.stopPropagation();
                    }
                });
                verticalPanel.add(timeoutMinutes);
                ParagraphElement timeoutMinutesDescription = Document.get().createPElement();
                timeoutMinutesDescription.setClassName("description");
                timeoutMinutesDescription
                    .setInnerText("Jenkins will wait this many minutes before terminating the build");
                verticalPanel.add(HTML.wrap(timeoutMinutesDescription));

                // JUnit Post Build Tasks
                HeadingElement junitHeader = Document.get().createHElement(2);
                junitHeader.setInnerText("JUnit");
                junitHeader.setClassName("subsection");
                verticalPanel.add(HTML.wrap(junitHeader));

                // enableJunit
                junitEnabled.setValue(false);
                junitEnabled.setEnabled(false);
                junitEnabled.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        updateWidgetEnablity();
                    }
                });
                verticalPanel.add(junitEnabled);
                ParagraphElement enableJunitDescription = Document.get().createPElement();
                enableJunitDescription.setClassName("description");
                enableJunitDescription
                    .setInnerText("Check this checkbox to enable publishing the report of JUnit test results");
                verticalPanel.add(HTML.wrap(enableJunitDescription));

                // junitResultsLocation
                ParagraphElement junitPathLabel = Document.get().createPElement();
                junitPathLabel.setInnerText("junit test results location");
                junitPathLabel.setClassName("label");
                verticalPanel.add(HTML.wrap(junitPathLabel));
                junitPath.setText("build/test-results/*.xml");
                junitPath.addKeyPressHandler(new KeyPressHandler() {
                    @Override
                    public void onKeyPress(KeyPressEvent event) {
                        event.stopPropagation();
                    }
                });
                verticalPanel.add(junitPath);
                ParagraphElement junitPathDescription = Document.get().createPElement();
                junitPathDescription.setClassName("description");
                junitPathDescription
                    .setInnerText("Path to use for junit results (e.g. build/test-results/*.xml");
                verticalPanel.add(HTML.wrap(junitPathDescription));

                // saveButton
                saveButton.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        String cronCheckString = cronJob.getText();
                        if(!cronCheckString.equals("success")){
                            alertWidget("ERROR: Time-Triggered Input Invalid", cronCheckString).center();
                            return;
                        }
                        Map<String, Object> params = new HashMap<String, Object>();
                        params.put("projectName", makeXMLFriendly(projectName));

                        params.put("verifyJobEnabled", verifyJobEnabled.getValue());
                        params.put("verifyBranchRegex", makeXMLFriendly(verifyBranchRegex.getText()));
                        params.put("verifyCommand", makeXMLFriendly(verifyCommand.getText()));

                        params.put("publishJobEnabled", publishJobEnabled.getValue());
                        params.put("publishBranchRegex", makeXMLFriendly(publishBranchRegex.getText()));
                        params.put("publishCommand", makeXMLFriendly(publishCommand.getText()));

                        params.put("cronJobEnabled", cronJobEnabled.getValue());
                        params.put("cronCommand", makeXMLFriendly(cronCommand.getText()));
                        params.put("cronJob", makeXMLFriendly(cronJob.getText()));

                        params.put("timeoutMinutes", Integer.valueOf(timeoutMinutes.getText()));
                        params.put("junitEnabled", junitEnabled.getValue());
                        params.put("junitPath", makeXMLFriendly(junitPath.getText()));

                        new RestApi("plugins").id("gerrit-ci").view("jobs")
                            .view(encodedProjectName)
                            .put((JavaScriptObject) params, new AsyncCallback<JavaScriptObject>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    // Never invoked. Errors are shown in a dialog.
                                }

                                @Override
                                public void onSuccess(JavaScriptObject result) {
                                    GetJobsResponseOverlay config = (GetJobsResponseOverlay) result;
                                    if(config.getErrorMsg() != null){
                                        alertWidget("ERROR: Action Not Completed",
                                                config.getErrorMsg().toString())
                                        .center();
                                       return;
                                    }
                                    alertWidget("Jenkins Server Response",
                                                "Jenkins has been updated successfully with your settings")
                                        .center();
                                }
                            });
                    }
                });
                saveButton.setEnabled(false);
                verticalPanel.add(saveButton);

                updateWidgetEnablity();

                screen.add(verticalPanel);
                screen.show();

                new RestApi("plugins").id("gerrit-ci").view("jobs").view(encodedProjectName)
                    .get(new AsyncCallback<JavaScriptObject>() {

                        @Override
                        public void onFailure(Throwable caught) {
                            // Never invoked. Errors are shown in a dialog.
                        }

                        @Override
                        public void onSuccess(JavaScriptObject result) {
                            GetJobsResponseOverlay config = (GetJobsResponseOverlay) result;
                            if(config.getErrorMsg() != null){
                                alertWidget("ERROR: Action Not Completed",
                                        config.getErrorMsg().toString())
                                .center();
                               return;
                            }
                            verifyJobEnabled.setValue(config.getVerifyJobEnabled());
                            publishJobEnabled.setValue(config.getPublishJobEnabled());
                            cronJobEnabled.setValue(config.getCronJobEnabled());
                            junitEnabled.setValue(config.getJunitEnabled());

                            String verifyBranchRegexString = makeXMLReadeable(config.getVerifyBranchRegex().toString());
                            String verifyCommandString = makeXMLReadeable(config.getVerifyCommand().toString());

                            String publishBranchRegexString =  makeXMLReadeable(config.getPublishBranchRegex().toString());
                            String publishCommandString =  makeXMLReadeable(config.getPublishCommand().toString());

                            String cronCommandString = makeXMLReadeable(config.getCronCommand().toString());
                            String cronJobString = makeXMLReadeable(config.getCronJob().toString());

                            Integer timeoutMinutesInteger = config.getTimeoutMinutes();

                            String junitPathString = makeXMLReadeable(config.getJunitPath().toString());

                            if(verifyBranchRegexString != null) {
                                verifyBranchRegex.setText(verifyBranchRegexString);
                            }

                            if(verifyCommandString != null) {
                                verifyCommand.setText(verifyCommandString);
                            }

                            if(publishBranchRegexString != null) {
                                publishBranchRegex.setText(publishBranchRegexString);
                            }

                            if(publishCommandString != null) {
                                publishCommand.setText(publishCommandString);
                            }

                            if(cronCommandString != null) {
                                cronCommand.setText(cronCommandString);
                            }

                            if(cronJobString != null) {
                                cronJob.setText(cronJobString);
                            }

                            if(timeoutMinutesInteger != null) {
                                timeoutMinutes.setText(String.valueOf(timeoutMinutesInteger));
                            }


                            if(junitPathString != null) {
                                junitPath.setText(junitPathString);
                            }

                            verifyJobEnabled.setEnabled(true);
                            publishJobEnabled.setEnabled(true);
                            cronJobEnabled.setEnabled(true);
                            junitEnabled.setEnabled(true);
                            saveButton.setEnabled(true);
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
        verifyCommand.setEnabled(verifyJobEnabled.getValue());

        publishBranchRegex.setEnabled(publishJobEnabled.getValue());
        publishCommand.setEnabled(publishJobEnabled.getValue());

        cronJob.setEnabled(cronJobEnabled.getValue());
        cronCommand.setEnabled(cronJobEnabled.getValue());

        timeoutMinutes.setEnabled(verifyJobEnabled.getValue() || publishJobEnabled.getValue());
        junitPath.setEnabled(junitEnabled.getValue());
    }

    /**
     * Creates a DialogBox object with the specified header and content and returns it to be
     * displayed.
     *
     * @param header Title for the alert dialog
     * @param content Inner message content to display
     * @return A DialogBox object configured with the specified strings
     */
    public static DialogBox alertWidget(final String header, final String content) {
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
        verticalPanel.setCellHorizontalAlignment(buttonClose, HasAlignment.ALIGN_CENTER);

        dialogBox.add(verticalPanel);
        dialogBox.setWidth("400px");
        return dialogBox;
    }

    public static String makeXMLFriendly(String s){
        if(s==null){
            return null;
        }
        return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

    }
    public static String makeXMLReadeable(String s){
        if (s==null)
            return null;
        return s.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">");

    }
}
