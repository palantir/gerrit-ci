package com.palantir.gerrit.gerritci.ui;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.webui.GerritTopMenu;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.inject.Inject;

/**
 * This class extends the Gerrit top menu and allows us to add sections and entries based on current
 * user permissions, project selection, and more.
 */
public class TopMenuExtension implements TopMenu {

    // Entries for the top menu. Sub-items for each entry are added to its MenuEntry object
    private final List<MenuEntry> menuEntries;

    @Inject
    public TopMenuExtension(@PluginName String name) {
        menuEntries = Lists.newArrayList();

        /*
         * By adding GerritTopMenu.PROJECTS, we are extending what is currently in the "Projects"
         * entry in the top menu with whatever MenuItems we add
         */
        menuEntries.add(new MenuEntry(GerritTopMenu.PROJECTS,
            Collections.singletonList(new MenuItem("Gerrit-CI", "#/x/" + name + "/settings"))));
    }

    @Override
    public List<MenuEntry> getEntries() {
        return menuEntries;
    }
}
