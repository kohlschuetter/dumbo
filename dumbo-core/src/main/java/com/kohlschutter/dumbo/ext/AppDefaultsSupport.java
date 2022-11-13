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
package com.kohlschutter.dumbo.ext;

import com.kohlschutter.dumbo.Extension;

/**
 * Simplifies app development by automatically providing references to the app's default JavaScript
 * and CSS files.
 */
public final class AppDefaultsSupport extends Extension {
  private final String js;
  private final String css;

  /**
   * Initializes this instance with default app JavaScript (/js/app.js) and CSS (/css/default.css)
   * resources.
   */
  public AppDefaultsSupport() {
    this("/js/app.js", "/css/default.css");
  }

  /**
   * Initializes this instance with the given default app JavaScript and CSS resources.
   *
   * @param js URL relative path to the app.js file
   * @param css URL relative path to the default CSS file
   */
  public AppDefaultsSupport(final String js, final String css) {
    this.css = css;
    this.js = js;
  }

  @Override
  protected void initResources() {
    registerCSS(css);
    registerJavaScript(js);
  }
}
