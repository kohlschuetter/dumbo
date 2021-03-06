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
package com.evernote.ai.dumbo;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import javax.servlet.http.HttpSession;

/**
 * Provides support for HTML/CSS/JavaScript-based extensions that simplify app
 * development.
 */
public abstract class Extension {
  private Collection<String> resJavaScript = new LinkedHashSet<>();
  private Collection<String> resAsyncJavaScript = new LinkedHashSet<>();
  private Collection<String> resCSS = new LinkedHashSet<>();

  private boolean initResourcesDone = false;

  /**
   * Called by the app to initialize the {@link Extension} for the given
   * {@link AppHTTPServer}.
   */
  final void doInit(AppHTTPServer app) {
    if (!initResourcesDone) {
      initResources();
      initResourcesDone = true;
    }
    init(app);
  }

  /**
   * Called to register any JavaScript and/or CSS resources that should be used by this
   * app.
   * 
   * @see #registerCSS(String)
   * @see #registerJavaScript(String)
   */
  protected abstract void initResources();

  /**
   * Registers a JavaScript resource by its HTTP path.
   * 
   * @param path The path.
   */
  protected final void registerJavaScript(final String path) {
    if (initResourcesDone) {
      throw new IllegalStateException("Cannot register resource at this point.");
    }
    resJavaScript.add(path);
  }

  /**
   * Registers an asynchronous JavaScript resource by its HTTP path.
   * 
   * @param path The path.
   */
  protected final void registerAsyncJavaScript(final String path) {
    if (initResourcesDone) {
      throw new IllegalStateException("Cannot register resource at this point.");
    }
    resAsyncJavaScript.add(path);
  }

  /**
   * Registers a CSS resource by its HTTP path.
   * 
   * @param path The path.
   */
  protected final void registerCSS(final String path) {
    if (initResourcesDone) {
      throw new IllegalStateException("Cannot register resource at this point.");
    }
    resCSS.add(path);
  }

  /**
   * Registers the extension's resources with the {@link AppHTTPServer}.
   * 
   * @param server The server instance to work with.
   */
  public void init(final AppHTTPServer server) {
  }

  /**
   * Returns an HTML string that may be added to the HTML HEAD section of a web page to
   * initialize this extension.
   * 
   * By default, this returns a set of {@code <LINK>} and/or {@code SCRIPT} HTML elements,
   * pointing to the resources registered in {@link #initResources()}.
   * 
   * @param context The context of the page.
   * @return The HTML string.
   */
  public String htmlHead(final HttpSession context) {
    StringBuilder sb = new StringBuilder();

    for (String path : resCSS) {
      sb.append("<link rel=\"stylesheet\" href=\"" + xmlEntities(path) + "\" />\n");
    }

    for (String path : resJavaScript) {
      sb.append("<script type=\"text/javascript\" src=\"" + xmlEntities(path)
          + "\"></script>\n");
    }
    for (String path : resAsyncJavaScript) {
      sb.append("<script type=\"text/javascript\" async=\"async\" src=\""
          + xmlEntities(path) + "\"></script>\n");
    }

    return sb.toString();
  }

  /**
   * Returns an HTML string that may be added to the top of the HTML BODY section of a web
   * page to initialize this extension.
   * 
   * @param context The context of the page.
   * @return The HTML string.
   */
  public String htmlBodyTop(final HttpSession context) {
    return null;
  }

  /**
   * Performs dependency resolution/checks.
   */
  public void resolveDependencies(final ServerApp app, List<Extension> extensions)
      throws ExtensionDependencyException {
  }

  private static String xmlEntities(final String in) {
    return in.replaceAll("&", "&amp;").replaceAll("\"", "&quot;").replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;").replaceAll("'", "&#39;");
  }
}
