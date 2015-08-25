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

import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class JenkinsJob extends JavaScriptObject {
  public static JenkinsJob create(String jobName, String jobType, ArrayList<JobParam> items) {
      JenkinsJob i = createObject().cast();
    i.jobName(jobName);
    i.jobType(jobType);
    i.addItems(items);
    return i;
  }
  public final native String getName() /*-{ return this.jobName; }-*/;
  public final native String getType() /*-{ return this.jobType; }-*/;
  public final native JsArray<JobParam> getItems() /*-{ return this.items; }-*/;

  final void addItems(ArrayList<JobParam>  items) {
      initItems();
      for (JobParam i : items) {
        addItem(i);
      }

    }
  public final native void jobName(String n) /*-{ this.jobName = n }-*/;
  public final native void jobType(String u) /*-{ this.jobType = u }-*/;
  final native void initItems() /*-{ this.items = []; }-*/;
  final native void addItem(JobParam i) /*-{ this.items.push(i); }-*/;

  protected JenkinsJob() {
  }
}