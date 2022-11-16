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
package com.kohlschutter.dumbo.console;

import java.net.URL;

import com.kohlschutter.dumbo.Extension;

/**
 * Helper class to add required console-specific resources to the demo server.
 *
 * See {@code dumbo-helloworld}.
 */
public class ConsoleSupport extends Extension {
  @Override
  protected void initResources() {
    registerCSS("css/app-console.css");
    registerJavaScript("js/app-console.js");
  }

  @Override
  protected String initExtensionPath() {
    return "/app_/base";
  }

  @Override
  protected URL initExtensionResourceURL() {
    return ConsoleSupport.class.getResource("/com/kohlschutter/dumbo/appbase/");
  }
}