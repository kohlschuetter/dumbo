/**
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
package com.evernote.ai.dumbo.ext;

import com.evernote.ai.dumbo.AppHTTPServer;
import com.evernote.ai.dumbo.Extension;

/**
 * Adds required base resources to the demo server, such as jQuery and json-rpc.
 */
public final class BaseSupport extends Extension {
  @Override
  public void init(final AppHTTPServer server) {
    server.registerContext("/_app_base", AppHTTPServer.class
        .getResource("/com/evernote/ai/dumbo/appbase/"));
  }

  @Override
  protected void initResources() {
    registerJavaScript("/_app_base/js/jquery.min.js");
    registerJavaScript("/_app_base/js/jsonrpc.js");
    registerJavaScript("/_app_base/js/app.js");
  }
}
