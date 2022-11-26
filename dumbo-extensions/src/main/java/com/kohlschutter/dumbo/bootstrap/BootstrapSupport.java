/*
 * Copyright 2022 Christian Kohlschütter
 * Copyright 2014,2015 Evernote Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kohlschutter.dumbo.bootstrap;

import com.kohlschutter.dumbo.annotations.CSSResource;
import com.kohlschutter.dumbo.annotations.CSSResources;
import com.kohlschutter.dumbo.annotations.Component;
import com.kohlschutter.dumbo.annotations.HTMLResource;
import com.kohlschutter.dumbo.annotations.HTMLResource.Target;
import com.kohlschutter.dumbo.annotations.HTMLResources;
import com.kohlschutter.dumbo.annotations.JavaScriptResource;
import com.kohlschutter.dumbo.annotations.JavaScriptResources;

/**
 * Helper class to add Bootstrap.js support to the demo server.
 *
 * See {@code dumbo-helloworld}.
 */
@CSSResources({//
    @CSSResource({"css/bootstrap.min.css", "css/bootstrap-extras.css"}) //
})
@JavaScriptResources({//
    @JavaScriptResource({"js/bootstrap.bundle.min.js", "js/bootstrap-extras.js"}) //
})
@HTMLResources({@HTMLResource(value = "include/noLongerCurrent.html", target = Target.BODY)})
public interface BootstrapSupport extends Component {

}
