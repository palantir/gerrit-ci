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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import java.util.List;

public class Jobs extends JavaScriptObject {
    public static Jobs create(List<JenkinsJob> items) {
        Jobs j = createObject().cast();
        j.setItems(items);
        return j;
    }

    protected Jobs() {
    }

    public final native JsArray<JenkinsJob> getItems() /*-{ return this.items; }-*/;

    public final native void name(String n) /*-{ this.name = n }-*/;

    final void setItems(List<JenkinsJob> items) {
        initItems();
        for (JenkinsJob i : items) {
            addItem(i);
        }

    }

    final native void initItems() /*-{ this.items = []; }-*/;

    final native void addItem(JenkinsJob i) /*-{ this.items.push(i); }-*/;
}
