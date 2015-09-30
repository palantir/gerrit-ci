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
