package com.palantir.gerrit.gerritci;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.webui.GerritTopMenu;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.inject.Inject;

public class TopMenuExtension implements TopMenu {
	private final List<MenuEntry> menuEntries;

	@Inject
	public TopMenuExtension(@PluginName String name) {
		menuEntries = Lists.newArrayList();
		menuEntries.add(new MenuEntry(GerritTopMenu.PROJECTS, Collections.singletonList(new MenuItem("Gerrit-CI", "#/x/" + name + "/settings"))));
	}
	
	@Override
	public List<MenuEntry> getEntries() {
		return menuEntries;
	}
}
