package com.palantir.gerrit.gerritci.ui.client;

import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.PluginEntryPoint;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.InlineLabel;

/**
 * This class shows the Gerrit-CI settings screen for a particular project to users with the proper
 * permissions.
 */
public class ProjectSettingsScreen extends PluginEntryPoint {

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

                screen.add(new InlineLabel("Gerrit-CI Project Settings for project: " + projectName));
                screen.show();
            }
        });
    }
}
