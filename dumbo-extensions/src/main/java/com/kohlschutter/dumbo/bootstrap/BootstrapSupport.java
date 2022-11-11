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

import java.util.Scanner;

import com.kohlschutter.dumbo.Extension;

import jakarta.servlet.http.HttpSession;

/**
 * Helper class to add Bootstrap.js support to the demo server.
 * 
 * See {@code dumbo-helloworld}.
 */
@SuppressWarnings("resource")
public final class BootstrapSupport extends Extension {
  private static final String HTML_NO_LONGER_CURRENT;
  private final boolean extras;

  static {
    HTML_NO_LONGER_CURRENT = new Scanner(BootstrapSupport.class.getResourceAsStream(
        "include/noLongerCurrent.html"), "UTF-8").useDelimiter("\\Z").next();
  }

  /**
   * Initializes bootstrap support with some custom additions.
   */
  public BootstrapSupport() {
    this(true);
  }

  /**
   * Initializes bootstrap support.
   * 
   * @param extras If {@code true}, custom additions will be enabled. If {@code false}, only the
   *          library is provided.
   */
  public BootstrapSupport(boolean extras) {
    this.extras = extras;
  }

  @Override
  protected void initResources() {
    registerCSS("css/bootstrap.min.css");
    registerCSS("css/bootstrap-extras.css");
    registerJavaScript("js/bootstrap.min.js");
    registerJavaScript("js/bootstrap-extras.js");
  }

  @Override
  public String htmlBodyTop(final HttpSession context) {
    if (extras) {
      return HTML_NO_LONGER_CURRENT;
    } else {
      return "";
    }
  }
}
