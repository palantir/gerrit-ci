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

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
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
        HTMLPanel cronPanel = new HTMLPanel("");
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
        HTMLPanel publishPanel = new HTMLPanel("");
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
        HTMLPanel verifyPanel = new HTMLPanel("");
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

    public static HTMLPanel showJob(){
        //TODO: Will return panel representing a JenkinsJobs
        return null;
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
