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
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
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
     * TextBox that contains the name of the command to run for publish jobs
     */
    private TextBox publishCommand;

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
                final String encodedProjectName =
                    Window.Location.getHash().replace("#/x/gerrit-ci/projects/", "");
                projectName = encodedProjectName.replace("%2F", "/");

                // Instantiate widgets
                VerticalPanel verticalPanel = new VerticalPanel();

                verifyJobEnabled = new CheckBox("Disabled");
                verifyBranchRegex = new TextBox();
                verifyCommand = new TextBox();
                publishJobEnabled = new CheckBox("Disabled");
                publishBranchRegex = new TextBox();
                publishCommand = new TextBox();
                timeoutEnabled = new CheckBox("Disabled");
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
                        boolean isEnabled = verifyJobEnabled.getValue();
                        verifyJobEnabled.setText(isEnabled ? "Enabled" : "Disabled");
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
                        boolean isEnabled = publishJobEnabled.getValue();
                        publishJobEnabled.setText(isEnabled ? "Enabled" : "Disabled");
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
                verticalPanel.add(publishBranchRegex);
                ParagraphElement publishBranchRegexDescription = Document.get().createPElement();
                publishBranchRegexDescription.setClassName("description");
                publishBranchRegexDescription
                    .setInnerText("Branches matching this regex will have the publish job command" +
                            " run on them by Jenkins. This regex must begin with \"refs/heads/\"");
                verticalPanel.add(HTML.wrap(publishBranchRegexDescription));

                // publishCommand
                ParagraphElement publishCommandLabel = Document.get().createPElement();
                publishCommandLabel.setInnerText("Command to run for a publish job");
                publishCommandLabel.setClassName("label");
                verticalPanel.add(HTML.wrap(publishCommandLabel));
                publishCommand.setText("./scripts/publish.sh");
                verticalPanel.add(publishCommand);
                ParagraphElement publishCommandDescription = Document.get().createPElement();
                publishCommandDescription.setClassName("description");
                publishCommandDescription
                    .setInnerText("Simple command or a shell script to run for publish jobs");
                verticalPanel.add(HTML.wrap(publishCommandDescription));

                // generalHeader
                HeadingElement generalHeader = Document.get().createHElement(2);
                generalHeader.setInnerText("General Settings");
                verticalPanel.add(HTML.wrap(generalHeader));

                // timeoutHeader
                HeadingElement timeoutHeader = Document.get().createHElement(2);
                timeoutHeader.setInnerText("Timeouts");
                timeoutHeader.setClassName("subsection");
                verticalPanel.add(HTML.wrap(timeoutHeader));

                // timeoutEnabled
                timeoutEnabled.setValue(false);
                timeoutEnabled.setEnabled(false);
                timeoutEnabled.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        boolean isEnabled = timeoutEnabled.getValue();
                        timeoutEnabled.setText(isEnabled ? "Enabled" : "Disabled");
                        updateWidgetEnablity();
                    }
                });
                verticalPanel.add(timeoutEnabled);
                ParagraphElement timeoutEnabledDescription = Document.get().createPElement();
                timeoutEnabledDescription.setClassName("description");
                timeoutEnabledDescription
                    .setInnerText("Check this checkbox to enable job timeouts for your project");
                verticalPanel.add(HTML.wrap(timeoutEnabledDescription));

                // timeoutMinutes
                ParagraphElement timeoutMinutesLabel = Document.get().createPElement();
                timeoutMinutesLabel.setInnerText("Timeout minutes");
                timeoutMinutesLabel.setClassName("label");
                verticalPanel.add(HTML.wrap(timeoutMinutesLabel));
                timeoutMinutes.setText("30");
                verticalPanel.add(timeoutMinutes);
                ParagraphElement timeoutMinutesDescription = Document.get().createPElement();
                timeoutMinutesDescription.setClassName("description");
                timeoutMinutesDescription
                    .setInnerText("Jenkins will wait this many minutes before terminating the job's run");
                verticalPanel.add(HTML.wrap(timeoutMinutesDescription));

                // saveButton
                saveButton.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        Map<String, Object> params = new HashMap<String, Object>();
                        params.put("projectName", projectName);

                        params.put("verifyJobEnabled", verifyJobEnabled.getValue());
                        params.put("verifyBranchRegex", verifyBranchRegex.getText());
                        params.put("verifyCommand", verifyCommand.getText());

                        params.put("publishJobEnabled", publishJobEnabled.getValue());
                        params.put("publishBranchRegex", publishBranchRegex.getText());
                        params.put("publishCommand", publishCommand.getText());

                        params.put("timeoutEnabled", timeoutEnabled.getValue());
                        params.put("timeoutMinutes", Integer.valueOf(timeoutMinutes.getText()));

                        new RestApi("plugins").id("gerrit-ci").view("jobs")
                            .view(encodedProjectName)
                            .put((JavaScriptObject) params, new AsyncCallback<JavaScriptObject>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    // Never invoked. Errors are shown in a dialog.
                                }

                                @Override
                                public void onSuccess(JavaScriptObject result) {
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
                            String verifyCommandString = config.getVerifyCommand();

                            String publishBranchRegexString = config.getPublishBranchRegex();
                            String publishCommandString = config.getPublishCommand();

                            Integer timeoutMinutesInteger = config.getTimeoutMinutes();

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

                            if(timeoutMinutesInteger != null) {
                                timeoutMinutes.setText(String.valueOf(timeoutMinutesInteger));
                            }

                            verifyJobEnabled.setEnabled(true);
                            publishJobEnabled.setEnabled(true);
                            timeoutEnabled.setEnabled(true);
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

        timeoutMinutes.setEnabled(timeoutEnabled.getValue());
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
        return dialogBox;
    }
}
