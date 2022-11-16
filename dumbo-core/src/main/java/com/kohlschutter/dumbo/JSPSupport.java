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

import jakarta.servlet.http.HttpSession;

/**
 * {@link Extension}-hooks to be called from within a JSP page.
 */
public final class JSPSupport {
  private JSPSupport() {
    throw new IllegalStateException("No instances");
  }

  /**
   * Returns an HTML string that may be added to the HTML HEAD section of a web page to initialize
   * all extensions registered with the app.
   *
   * @param session The HTTP session associated with the page.
   * @return The HTML string.
   */
  public static String htmlHead(final HttpSession session) {
    ServerApp app = getApp(session);
    if (app == null) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    for (Extension ext : app.getExtensions()) {
      String v = ext.htmlHead(session);
      if (v != null) {
        sb.append(v);
      }
    }

    return sb.toString();
  }

  /**
   * Returns an HTML string that may be added to the top of the HTML BODY section of a web page to
   * initialize all extensions registered with the app.
   *
   * @param session The HTTP session associated with the page.
   * @return The HTML string.
   */
  public static String htmlBodyTop(final HttpSession session) {
    ServerApp app = getApp(session);
    if (app == null) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    for (Extension ext : app.getExtensions()) {
      String v = ext.htmlBodyTop(session);
      if (v != null) {
        sb.append(v);
      }
    }

    return sb.toString();
  }

  /**
   * Returns the {@link ServerApp} associated with the given {@link HttpSession}.
   *
   * @param session The session to check.
   * @return The instance.
   */
  public static ServerApp getApp(final HttpSession session) {
    return (ServerApp) session.getServletContext().getAttribute(ServerApp.class.getName());
  }
}