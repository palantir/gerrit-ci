// Copyright (C) 2014 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.palantir.gerrit.gerritci.ui.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

public interface Resources extends ClientBundle {

    @Source("info.png")
    public ImageResource info();

    @Source("cronDescription.html")
    TextResource cron();

    @Source("cronPanel.html")
    TextResource cronJobPanel();

    @Source("publishJobPanel.html")
    TextResource publishJobPanel();

    @Source("verifyJobPanel.html")
    TextResource verifyJobPanel();

    @Source("buttonsPanel.html")
    TextResource buttonsPanel();


}
