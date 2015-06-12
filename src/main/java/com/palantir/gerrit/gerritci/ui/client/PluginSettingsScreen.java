package com.palantir.gerrit.gerritci.ui.client;

import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.PluginEntryPoint;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.user.client.ui.InlineLabel;

/**
 * This class shows the admin settings screen to users with the proper permissions.
 */
public class PluginSettingsScreen extends PluginEntryPoint {

    @Override
    public void onPluginLoad() {
        Plugin.get().screen("settings", new Screen.EntryPoint() {

            @Override
            public void onLoad(Screen screen) {
                screen.add(new InlineLabel("Gerrit-CI Admin Settings"));
                screen.show();
            }
        });
    }
}
