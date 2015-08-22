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
