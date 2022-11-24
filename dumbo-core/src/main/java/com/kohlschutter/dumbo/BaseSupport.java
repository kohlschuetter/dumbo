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
package com.kohlschutter.dumbo;

import java.net.URL;

/**
 * Adds required base resources, such as jQuery and json-rpc.
 */
@Servlets({ //
    @ServletMapping(map = "*.js", to = JspJsServlet.class, initOrder = 0),
    @ServletMapping(map = "*.jsp", to = JspCachingServlet.class, initOrder = 0),
    //
})
final class BaseSupport extends Extension {

  @Override
  protected void initResources() {
    registerJavaScript("js/jquery.min.js");
    registerJavaScript("js/jsonrpc.js");
    registerJavaScript("js/app.js");
  }

  @Override
  protected String initExtensionPath() {
    return "/app_/base";
  }

  @Override
  protected URL initExtensionResourceURL() {
    return BaseSupport.class.getResource("/com/kohlschutter/dumbo/appbase/");
  }
}
