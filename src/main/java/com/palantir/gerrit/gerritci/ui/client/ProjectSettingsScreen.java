package com.palantir.gerrit.gerritci.ui.client;

import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.PluginEntryPoint;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * This class shows the Gerrit-CI settings screen for a particular project to users with the proper
 * permissions.
 */
public class ProjectSettingsScreen extends PluginEntryPoint {

    /**
     * Main panel into which we will place all other panels and widgets for this screen
     */
    private VerticalPanel verticalPanel;

    /**
     * Big header that displays the project name
     */
    private InlineLabel header;

    /**
     * CheckBox that will enable or disable all types of Jenkins jobs for the current project
     */
    private CheckBox jobsEnabled;

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
                String projectName = History.getToken().replace("/x/gerrit-ci/projects/", "");

                // Instantiate widgets
                verticalPanel = new VerticalPanel();
                header = new InlineLabel("Gerrit-CI Settings for Project: " + projectName);
                jobsEnabled = new CheckBox("Disabled");
                verifyJobEnabled = new CheckBox("Disabled");
                verifyBranchRegex = new TextBox();
                publishJobEnabled = new CheckBox("Disabled");
                publishBranchRegex = new TextBox();

                // header
                header.getElement().getStyle().setFontSize(20, Unit.PX);

                // jobsEnabled
                jobsEnabled.setValue(false);
                jobsEnabled.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        boolean isEnabled = jobsEnabled.getValue();

                        jobsEnabled.setText(isEnabled ? "Enabled" : "Disabled");
                        updateWidgetEnablity();
                    }
                });

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

                // verifyBranchRegex
                verifyBranchRegex.setText(".*");

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

                // publishBranchRegex
                publishBranchRegex.setText("refs/heads/(develop|master)");

                updateWidgetEnablity();

                // Add all widgets to the main VerticalPanel
                verticalPanel.add(header);
                verticalPanel.add(jobsEnabled);
                verticalPanel.add(verifyJobEnabled);
                verticalPanel.add(verifyBranchRegex);
                verticalPanel.add(publishJobEnabled);
                verticalPanel.add(publishBranchRegex);

                screen.add(verticalPanel);
                screen.show();
            }
        });
    }

    /**
     * To be called after updating the enability (yes, I invented a word to describe the state of
     * being enabled/disabled) of a CheckBox. This method will update the enability of other
     * widgets based on the change.
     */
    private void updateWidgetEnablity() {
        if(jobsEnabled.getValue()) {
            verifyJobEnabled.setEnabled(true);
            publishJobEnabled.setEnabled(true);

            verifyBranchRegex.setEnabled(verifyJobEnabled.getValue());
            publishBranchRegex.setEnabled(publishJobEnabled.getValue());
        } else {
            verifyJobEnabled.setEnabled(false);
            verifyBranchRegex.setEnabled(false);
            publishJobEnabled.setEnabled(false);
            publishBranchRegex.setEnabled(false);
        }
    }
}
