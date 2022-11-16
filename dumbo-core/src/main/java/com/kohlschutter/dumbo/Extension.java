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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.webapp.WebAppContext;

import jakarta.servlet.http.HttpSession;

/**
 * Provides support for HTML/CSS/JavaScript-based extensions that simplify app development.
 */
public abstract class Extension {
  private final String extensionPath;
  private String serverContextPath;
  private String contextPath;

  private final Collection<String> resJavaScript = new LinkedHashSet<>();
  private final Collection<String> resAsyncJavaScript = new LinkedHashSet<>();
  private final Collection<String> resCSS = new LinkedHashSet<>();

  private final AtomicBoolean initialized = new AtomicBoolean();
  private boolean initResourcesDone = false;
  private String htmlHead;

  protected Extension() {
    this.extensionPath = initPath();
  }

  private String initPath() {
    String path = initExtensionPath();
    if (path == null) {
      path = "";
    } else {
      path = path.trim().replaceAll("[/ ]+$", "");
    }
    if (path.isEmpty()) {
      return null;
    } else {
      return path;
    }
  }

  protected String initExtensionPath() {
    String name = getClass().getName();
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    return "/app_/" + Base64.getEncoder().encodeToString(md.digest(name.getBytes(
        StandardCharsets.UTF_8))).replace('/', '_');
  }

  /**
   * Called by the app to initialize the {@link Extension} for the given {@link AppHTTPServer}.
   * 
   * @throws IOException on error.
   */
  final void doInit(AppHTTPServer app) throws IOException {
    if (!initialized.compareAndSet(false, true)) {
      return;
    }
    if (!initResourcesDone) {
      initResources();
      initResourcesDone = true;
    }
    init(app);
  }

  /**
   * Called to register any JavaScript and/or CSS resources that should be used by this app.
   *
   * @see #registerCSS(String)
   * @see #registerJavaScript(String)
   */
  protected abstract void initResources();

  private void register(Collection<String> targetCollection, String path) {
    if (initResourcesDone) {
      throw new IllegalStateException("Cannot register resource at this point.");
    }
    targetCollection.add(path);
  }

  /**
   * Registers a JavaScript resource by its HTTP path.
   *
   * @param path The path.
   */
  protected final void registerJavaScript(final String path) {
    register(resJavaScript, path);
  }

  /**
   * Registers an asynchronous JavaScript resource by its HTTP path.
   *
   * @param path The path.
   */
  protected final void registerAsyncJavaScript(final String path) {
    register(resAsyncJavaScript, path);
  }

  /**
   * Registers a CSS resource by its HTTP path.
   *
   * @param path The path.
   */
  protected final void registerCSS(final String path) {
    register(resCSS, path);
  }

  /**
   * Registers the extension's resources with the {@link AppHTTPServer}.
   *
   * @param server The server instance to work with.
   * @throws IOException on error.
   */
  public void init(final AppHTTPServer server) throws IOException {
    serverContextPath = server.getContextPath().replaceFirst("/$", "");
    contextPath = serverContextPath;
    if (extensionPath != null) {
      URL webappUrl = initExtensionResourceURL();
      if (webappUrl != null) {
        WebAppContext wac = server.registerContext(extensionPath, webappUrl);
        contextPath = wac.getContextPath().replaceFirst("/$", "");
      }
    }

    htmlHead = this.initHtmlHead();
  }

  /**
   * Returns the resource URL containing the web content for this extension.
   *
   * @return The URL (default to the "webapp/" folder relative to the extension's class name)
   */
  protected URL initExtensionResourceURL() {
    return getClass().getResource("webapp/");
  }

  private String toAbsolutePath(String path) {
    if (path.contains("://")) {
      return path;
    } else if (path.startsWith("/")) {
      return serverContextPath + path;
    } else {
      return contextPath + "/" + path;
    }
  }

  private String initHtmlHead() {
    StringBuilder sb = new StringBuilder();

    for (String path : resCSS) {
      sb.append("<link rel=\"stylesheet\" href=\"" + xmlEntities(toAbsolutePath(path)) + "\" />\n");
    }

    for (String path : resJavaScript) {
      sb.append("<script type=\"text/javascript\" src=\"" + xmlEntities(toAbsolutePath(path))
          + "\"></script>\n");
    }
    for (String path : resAsyncJavaScript) {
      sb.append("<script type=\"text/javascript\" async=\"async\" src=\"" + xmlEntities(
          toAbsolutePath(path)) + "\"></script>\n");
    }

    return sb.toString();
  }

  /**
   * Returns an HTML string that may be added to the HTML HEAD section of a web page to initialize
   * this extension.
   *
   * By default, this returns a set of {@code <LINK>} and/or {@code SCRIPT} HTML elements, pointing
   * to the resources registered in {@link #initResources()}.
   *
   * @param context The context of the page.
   * @return The HTML string.
   */
  public String htmlHead(final HttpSession context) {
    return Objects.requireNonNull(htmlHead);
  }

  /**
   * Returns an HTML string that may be added to the top of the HTML BODY section of a web page to
   * initialize this extension.
   *
   * @param context The context of the page.
   * @return The HTML string.
   */
  public String htmlBodyTop(final HttpSession context) {
    return null;
  }

  /**
   * Performs dependency resolution/checks.
   *
   * @param app The server app
   * @param extensions The extensions to check.
   * @throws ExtensionDependencyException on dependency conflict.
   */
  public void resolveDependencies(final ServerAppBase app, List<Extension> extensions)
      throws ExtensionDependencyException {
  }

  private static String xmlEntities(final String in) {
    return in.replaceAll("&", "&amp;").replaceAll("\"", "&quot;").replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;").replaceAll("'", "&#39;");
  }

  String getContextPath() {
    return contextPath;
  }

  String getExtensionPath() {
    return extensionPath;
  }
}
