/*
 * Copyright 2022-2025 Christian Kohlschütter
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

import java.util.Set;

import com.kohlschutter.dumbo.api.DumboComponent;

import jakarta.servlet.http.HttpSession;

/**
 * {@link ExtensionImpl}-hooks to be called from within a JSP page.
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
    } else {
      return htmlHead(app);
    }
  }

  @SafeVarargs
  public static void markComponentUsed(final HttpSession session,
      Class<? extends DumboComponent>... components) {
    ServerApp app = getApp(session);
    if (app == null) {
      return;
    }
    RenderState renderState = RenderState.get();
    renderState.setApp(app);

    for (Class<? extends DumboComponent> component : components) {
      renderState.setMarkedUsed(component);

      Set<Class<? extends DumboComponent>> subcomponents = app.getComponentToSubComponentMap().get(
          component);
      if (subcomponents != null && !subcomponents.isEmpty()) {
        for (Class<? extends DumboComponent> subcomponent : subcomponents) {
          renderState.setMarkedUsed(subcomponent);
        }
      }
    }
  }

  /**
   * Returns an absolute path (from the server root) for a path relative to the given component
   * class.
   * 
   * @param session The current Http session.
   * @param componentClass The reference component class
   * @param relativePath The path relative to the component's root.
   * @return The absolute path.
   */
  public static String componentPath(final HttpSession session,
      Class<? extends DumboComponent> componentClass, String relativePath) {
    ServerApp app = getApp(session);
    if (app == null) {
      return null; // FIXME
    }
    ExtensionImpl ext = app.getExtensionByClass(componentClass);
    if (ext == null) {
      return null; // FIXME
    }
    if (!relativePath.startsWith("/")) {
      relativePath = "/" + relativePath;
    }
    return app.getContextPath() + ext.getExtensionPath() + relativePath; // FIXME
  }

  public static String htmlHead(final ServerApp app) {
    // RenderState.get().setMarkedUsedAllComponents(true);
    return ExtensionResourceHelper.htmlHead(app).toString();
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
    } else {
      return htmlBodyTop(app);
    }
  }

  public static String htmlBodyTop(final ServerApp app) {
    // RenderState.get().setMarkedUsedAllComponents(true);
    return ExtensionResourceHelper.htmlBodyTop(app).toString();
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