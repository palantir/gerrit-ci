package com.palantir.gerrit.gerritci.ui.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ProjectConfigurationScreen extends VerticalPanel {
    /**
     * The name of the currently-selected project
     */
    private static String projectName;
    private static String encodedProjectName;
    public static HashMap<String, Map<String, String>> jobsList;
    public static HashSet<HTMLPanel> activePanels;
    private Button saveButton;
    private Button addCronJob;
    private Button addPublishJob;
    private Button addVerifyJob;

    static class Factory implements Screen.EntryPoint {
        @Override
        public void onLoad(Screen screen) {
            encodedProjectName = Window.Location.getHash().replace("#/x/gerrit-ci/projects/", "");
            projectName = encodedProjectName.replace("%2F", "/");
            jobsList = new HashMap<String, Map<String, String>>();
            activePanels = new HashSet<HTMLPanel>();
            screen.show(new ProjectConfigurationScreen(projectName, Unit.EM));
        }
    }

    ProjectConfigurationScreen(String title, Unit u) {
        setStyleName("gerrit-ci");
        add(new HTML("Project Title: " + title));

        new RestApi("plugins").id("gerrit-ci").view("jobs").view(encodedProjectName)
        .get(new AsyncCallback<Jobs>() {
            @Override
            public void onFailure(Throwable caught) {
                // Never invoked. Errors are shown in a dialog.
            }

            @Override
            public void onSuccess(Jobs result) {
                if ((result.getItems() != null && result.getItems().get(0).getType().equals("ERROR"))) {
                    try {
                        String msg = result.getItems().get(0).getItems().get(0).getVal();
                        alertWidget("Action not completed", msg).center();
                    } catch (Exception e) {
                        alertWidget(
                                "Action not completed",
                                "Please verify connection to Jenkins" + " is valid in the gerrit-ci admin page "
                                        + "(administrator is access required).").center();
                    }

                } else {
                    int numOfJobs = result.getItems().length();
                    for (int i = 0; i < numOfJobs; i++) {
                        if (result.getItems().get(0).getItems().length() > 0) {
                            JenkinsJob j = result.getItems().get(i);
                            final HTMLPanel p = JobPanels.showJob(j);
                            if (p != null) {
                                Button deleteButton = new Button("delete");
                                deleteButton.addClickHandler(new ClickHandler() {
                                    @Override
                                    public void onClick(ClickEvent event) {
                                        deletePanel(p);
                                    }
                                });
                                p.add(deleteButton);
                                add(p);
                                activePanels.add(p);
                            }
                        }
                    }
                }
            }
        });
        addVerifyJob = new Button("+ Verify Job");
        addVerifyJob.setEnabled(true);
        addVerifyJob.setStyleName("add");
        addVerifyJob.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                add(createJob("verify"));
            }
        });
        addPublishJob = new Button("+ Publish Job");
        addPublishJob.setEnabled(true);
        addPublishJob.setStyleName("add");
        addPublishJob.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                add(createJob("publish"));
            }
        });
        addCronJob = new Button("+ Time-Triggered Job");
        addCronJob.setEnabled(true);
        addCronJob.setStyleName("add");
        addCronJob.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                add(createJob("cron"));
            }
        });
        HTMLPanel buttonPanel = new HTMLPanel(GerritCiPlugin.buttonsPanel.toString());
        buttonPanel.add(addVerifyJob, "addVerifyJob");
        buttonPanel.add(addCronJob, "addCronJob");
        buttonPanel.add(addPublishJob, "addPublishJob");
        buttonPanel.add(saveButton, "saveButton");
        add(buttonPanel);
    }

    private void deletePanel(HTMLPanel p) {
        activePanels.remove(p);
        remove(p);
    }

    private HTMLPanel createJob(String jobType) {
        final HTMLPanel p = JobPanels.createJobPanel(jobType);
        TextBox jobName = new TextBox();
        jobName.setName("jobName");
        jobName.setText(jobType + "_" + projectName + "_" + Random.nextInt(3));
        jobName.setVisible(false);
        p.add(jobName);
        Button deleteButton = new Button("delete");
        deleteButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                deletePanel(p);
            }
        });
        p.addAndReplaceElement(deleteButton, "delete");
        activePanels.add(p);
        return p;
    }

    private static void updateActiveJobs() {
        for (HTMLPanel panel : activePanels) {
            Map<String, String> jobParams = JobPanels.getValueMap(panel);
            String jobName = jobParams.get("jobName");
            jobParams.remove("jobName");
            jobsList.put(jobName, jobParams);
        }
    }

    private void doSave() {

        updateActiveJobs();

        List<JenkinsJob> jobs = new ArrayList<>();

        for (String jobName : jobsList.keySet()) {
            ArrayList<JobParam> params = new ArrayList<JobParam>();
            String jobType = jobsList.get(jobName).get("jobType");
            for (String field : jobsList.get(jobName).keySet()){
                if(field.equals("cronJob")){
                    String cronCheck = cronCheck(jobsList.get(jobName).get(field));
                    if(!cronCheck.equals("success")){
                        alertWidget("Warning", cronCheck).center();
                        return;
                    }
                }
                else if(field.equals("timeoutMinutes")){
                    String minutesCheck = timeoutCheck(jobsList.get(jobName).get(field));
                    if(!minutesCheck.equals("success")){
                        alertWidget("Warning", minutesCheck).center();
                        return;
                    }
                }
                params.add(JobParam.create(field, jobsList.get(jobName).get(field)));
            }
            jobs.add(JenkinsJob.create(jobName, jobType, params));
        }

        if (jobs.isEmpty()) {
            ArrayList<JobParam> params = new ArrayList<JobParam>();
            jobs.add(JenkinsJob.create("DeleteAll", "DeleteAll", params));
        }

        Jobs input = Jobs.create(jobs);

        new RestApi("plugins").id("gerrit-ci").view("jobs").view(encodedProjectName).put(input, new AsyncCallback<Jobs>() {
            @Override
            public void onFailure(Throwable caught) {
                // Never invoked. Errors are shown in a dialog.
            }

            @Override
            public void onSuccess(Jobs result) {
                if (result != null && result.getItems() != null && result.getItems().get(0).getType().equals("ERROR")) {
                    String field = result.getItems().get(0).getItems().get(0).getField();
                    String msg = result.getItems().get(0).getItems().get(0).getVal();
                    alertWidget("ERROR: Action not completed", "field: " + field + " message" + msg).center();
                }
                alertWidget("Jenkins Server Response", "Jenkins has been updated successfully with your settings").center();
            }
        });
    }

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

}
