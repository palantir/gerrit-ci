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
import java.util.HashSet;
import java.util.Map;

import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ProjectConfigurationScreen extends VerticalPanel {
    /**
     * The name of the currently-selected project
     */
    private static String projectName;
    private static String encodedProjectName;
    public static HashMap<String, Map<String, String>> cronJobsList;
    public static HashMap<String, Map<String, String>> verifyJobsList;
    public static HashMap<String, Map<String, String>> publishJobsList;
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
            cronJobsList = new HashMap<String, Map<String, String>>();
            verifyJobsList = new HashMap<String, Map<String, String>>();
            publishJobsList = new HashMap<String, Map<String, String>>();
            activePanels = new HashSet<HTMLPanel>();
            screen.show(new ProjectConfigurationScreen(projectName, Unit.EM));
        }
    }

    ProjectConfigurationScreen(String title, Unit u) {
        setStyleName("gerrit-ci");
        add(new HTML("Project Title: " + title));
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
