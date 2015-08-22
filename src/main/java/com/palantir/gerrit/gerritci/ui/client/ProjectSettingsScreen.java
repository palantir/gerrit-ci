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

import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.PluginEntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;

/**
 * This class shows the Gerrit-CI settings screen for a particular project to users with the proper
 * permissions.
 */
public class ProjectSettingsScreen extends PluginEntryPoint {

    public static final Resources RESOURCES = GWT.create(Resources.class);
    public static final HTMLPanel buttonsPanel = new HTMLPanel(ProjectSettingsScreen.RESOURCES.buttonsPanel().getText());
    public static final HTMLPanel cronToggle = new HTMLPanel(ProjectSettingsScreen.RESOURCES.cron().getText());
    public static final HTMLPanel cronPanel = new HTMLPanel(ProjectSettingsScreen.RESOURCES.cronJobPanel().getText());
    public static final HTMLPanel publishJobPanel = new HTMLPanel(ProjectSettingsScreen.RESOURCES.publishJobPanel().getText());
    public static final HTMLPanel verifyJobPanel = new HTMLPanel(ProjectSettingsScreen.RESOURCES.verifyJobPanel().getText());
    public static final Image cronToggleButton = new Image(ProjectSettingsScreen.RESOURCES.info());

    @Override
    public void onPluginLoad() {

        //Screen to configure settings to the gerrit-ci Plugin
        Plugin.get().screen("settings", new ConfigurationScreen.Factory());
        /*
         * The regex will match all strings following "projects/". This is necessary because project
         * names can contain slashes.
         */
        Plugin.get().screenRegex("projects/.+", new ProjectConfigurationScreen.Factory());;
    }
}
