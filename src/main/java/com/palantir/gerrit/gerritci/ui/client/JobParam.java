package com.palantir.gerrit.gerritci.ui.client;

import com.google.gwt.core.client.JavaScriptObject;

public class JobParam extends JavaScriptObject {
    public static JobParam create(String field, Object value) {
        JobParam i = createObject().cast();
        i.field(field);
        i.value(value);
        return i;
    }

    public final native String getField() /*-{ return this.field; }-*/;

    public final native String getVal() /*-{ return this.value; }-*/;

    public final native void field(String n) /*-{ this.field = n }-*/;

    public final native void value(Object u) /*-{ this.value = u }-*/;

    protected JobParam() {
    }
}
