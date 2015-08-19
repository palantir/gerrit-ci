package com.palantir.gerrit.gerritci.ui.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class JobPanels {

    public static HTMLPanel createJobPanel(String jobType) {
        if (jobType.equals("cron")) {
            return getCronPanel();
        }
        if(jobType.equals("publish"))
            return getPublishPanel();
        if(jobType.equals("verify"))
            return getVerifyPanel();
        return null;
    }

    //Returns a Cron Job Panel with default values.
    private static HTMLPanel getCronPanel() {
        HTMLPanel cronPanel = new HTMLPanel(GerritCiPlugin.cronPanel.toString());
        TextBox cronCommand = new TextBox();
        cronCommand.setName("cronCommand");
        cronCommand.setText("./scripts/cron.sh");
        TextBox cronSchedule = new TextBox();
        cronSchedule.setName("cronJob");
        TextBox jobType = new TextBox();
        jobType.setText("cron");
        jobType.setName("jobType");
        jobType.setVisible(false);
        cronPanel.add(jobType);
        cronPanel.addAndReplaceElement(cronCommand, "cronCommand");
        cronPanel.addAndReplaceElement(cronSchedule, "cronJob");
        addCommonFields(cronPanel);
        return cronPanel;
    }

    private static HTMLPanel getPublishPanel() {
        HTMLPanel publishPanel = new HTMLPanel(GerritCiPlugin.publishJobPanel.toString());
        TextBox publishCommand = new TextBox();
        publishCommand.setName("publishCommand");
        publishCommand.setText("./scripts/publish.sh");
        TextBox publishBranchRegex = new TextBox();
        publishBranchRegex.setName("publishBranchRegex");
        publishBranchRegex.setText("refs/heads/(develop|master)");
        TextBox jobType = new TextBox();
        jobType.setText("publish");
        jobType.setName("jobType");
        jobType.setVisible(false);
        publishPanel.add(jobType);
        publishPanel.addAndReplaceElement(publishCommand, "publishCommand");
        publishPanel.addAndReplaceElement(publishBranchRegex, "publishBranchRegex");
        addCommonFields(publishPanel);
        return publishPanel;
    }

    private static HTMLPanel getVerifyPanel() {
        HTMLPanel verifyPanel = new HTMLPanel(GerritCiPlugin.verifyJobPanel.toString());
        TextBox verifyCommand = new TextBox();
        verifyCommand.setName("verifyCommand");
        verifyCommand.setText("./scripts/verify.sh");
        TextBox verifyBranchRegex = new TextBox();
        verifyBranchRegex.setName("verifyBranchRegex");
        verifyBranchRegex.setText(".*");
        TextBox jobType = new TextBox();
        jobType.setText("verify");
        jobType.setName("jobType");
        jobType.setVisible(false);
        verifyPanel.add(jobType);
        verifyPanel.addAndReplaceElement(verifyCommand, "verifyCommand");
        verifyPanel.addAndReplaceElement(verifyBranchRegex, "verifyBranchRegex");
        addCommonFields(verifyPanel);
        return verifyPanel;
    }


    //Adds general setting to an HTMLPanel
    public static void addCommonFields(HTMLPanel p){
        CheckBox junitEnabled = new CheckBox("Publish JUnit test result report");
        junitEnabled.setName("junitEnabled");
        junitEnabled.setValue(true);
        TextBox junitPath = new TextBox();
        junitPath.setText("build/test-results/*.xml");
        junitPath.setName("junitPath");
        TextBox timeoutMinutes = new TextBox();
        timeoutMinutes.setText("30");
        timeoutMinutes.setName("timeoutMinutes");
        p.addAndReplaceElement(junitEnabled, "junitEnabled");
        p.addAndReplaceElement(junitPath,"junitPath");
        p.addAndReplaceElement(timeoutMinutes, "timeoutMinutes");
    }

    //Returns TextBox and CheckBox values
    public static Map<String,String> getValueMap(HTMLPanel p){
        HashMap<String,String> vals = new HashMap<String, String>();
        for (Widget w: p) {
            if (w instanceof TextBox) {
             vals.put(((TextBox)w).getName(),makeXMLFriendly(((TextBox)w).getValue()));
            }
            if (w instanceof CheckBox) {
                vals.put(((CheckBox)w).getName(),((CheckBox)w).getValue().toString());
           }
         }
        return vals;
    }

    public static HTMLPanel showJob(JenkinsJob j){
        String name = j.getName();
        HTMLPanel p = new HTMLPanel("");
        if(j.getType().equals("cron"))
            p = new HTMLPanel(GerritCiPlugin.cronPanel.toString());
        else if(j.getType().equals("publish"))
            p = new HTMLPanel(GerritCiPlugin.publishJobPanel.toString());
        else if(j.getType().equals("verify"))
            p = new HTMLPanel(GerritCiPlugin.verifyJobPanel.toString());
        else
            return null;
        TextBox jobName = new TextBox();
        jobName.setName("jobName");
        jobName.setText(name);
        jobName.setVisible(false);
        Label jobNameLabel = new Label("Job Id: " + name);
        p.add(jobNameLabel);
        p.add(jobName);
        TextBox jobType = new TextBox();
        jobType.setName("jobType");
        jobType.setText(j.getType());
        jobType.setVisible(false);
        p.add(jobType);

        int numOfParams = j.getItems().length();

        for (int i = 0; i < numOfParams; i++) {
            JobParam jp = j.getItems().get(i);
            String field = jp.getField();
            String value = jp.getVal();
            if (field.endsWith("Enabled")) {
                CheckBox cb = new CheckBox();
                cb.setName(field);
                cb.setValue(Boolean.valueOf(value));
                if (p.getElementById(field) != null)
                    p.addAndReplaceElement(cb, field);
                else {
                    cb.setVisible(false);
                    p.add(cb);
                }
            } else {
                TextBox tb = new TextBox();
                tb.setName(field);
                tb.setText(makeXMLReadeable(value));
                if (p.getElementById(field) != null)
                    p.addAndReplaceElement(tb, field);
                else {
                    tb.setVisible(false);
                    p.add(tb);
                }
            }
        }
        p.setVisible(true);
        return p;
    }

    public static String makeXMLFriendly(String s){
        return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

    }
    public static String makeXMLReadeable(String s){
        if (s==null)
            return null;
        return s.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">");

    }

}
