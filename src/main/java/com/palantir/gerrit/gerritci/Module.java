package com.palantir.gerrit.gerritci;

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.inject.AbstractModule;
import com.palantir.gerrit.gerritci.ui.TopMenuExtension;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        DynamicSet.bind(binder(), TopMenu.class).to(TopMenuExtension.class);
    }
}
