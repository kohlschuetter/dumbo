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
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.eclipse.jetty.webapp.WebAppContext;

import com.kohlschutter.dumbo.annotations.CSSResource;
import com.kohlschutter.dumbo.annotations.CSSResources;
import com.kohlschutter.dumbo.annotations.Component;
import com.kohlschutter.dumbo.annotations.HTMLResource;
import com.kohlschutter.dumbo.annotations.HTMLResource.Target;
import com.kohlschutter.dumbo.annotations.HTMLResources;
import com.kohlschutter.dumbo.annotations.JavaScriptResource;
import com.kohlschutter.dumbo.annotations.JavaScriptResources;
import com.kohlschutter.dumbo.exceptions.ExtensionDependencyException;
import com.kohlschutter.dumbo.util.NameObfuscator;

import jakarta.servlet.http.HttpSession;

/**
 * Provides support for HTML/CSS/JavaScript-based extensions that simplify app development.
 */
final class ExtensionImpl extends ComponentImpl {
  private final String extensionPath;
  private String serverContextPath;
  private String contextPath;

  private final Collection<String> resJavaScript = new LinkedHashSet<>();
  private final Collection<String> resAsyncJavaScript = new LinkedHashSet<>();
  private final Collection<String> resCSS = new LinkedHashSet<>();
  private final Collection<String> resHEAD = new LinkedHashSet<>();
  private final Collection<String> resBODY = new LinkedHashSet<>();

  private final AtomicBoolean initialized = new AtomicBoolean();
  private String htmlHead = "";
  private String htmlBodyTop = "";

  ExtensionImpl(Class<? extends Component> comp) {
    super(comp);
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

  String initExtensionPath() {
    ServletContextPath path = getMostRecentComponentAnnotation(ServletContextPath.class);
    if (path != null) {
      return path.value();
    }

    String name = getComponentClass().getName();

    return "/app_/" + NameObfuscator.obfuscate(name);
  }

  /**
   * Called by the app to initialize the {@link ExtensionImpl} for the given {@link AppHTTPServer}.
   *
   * @throws IOException on error.
   */
  void doInit(AppHTTPServer app) throws IOException {
    if (!initialized.compareAndSet(false, true)) {
      throw new IllegalStateException("Already initialized");
    }
    initResources();
    init(app);
  }

  /**
   * Called to register any JavaScript and/or CSS resources that should be used by this app.
   *
   * @see #registerCSS(String)
   * @see #registerJavaScript(String)
   */
  private void initResources() {
    List<JavaScriptResource> jsResources = getComponentAnnotations(JavaScriptResource.class);
    getComponentAnnotations(JavaScriptResources.class).stream().map((k) -> k.value()).flatMap(
        Stream::of).collect(() -> jsResources, (t, u) -> t.add(u), (t, u) -> t.addAll(u));

    List<CSSResource> cssResources = getComponentAnnotations(CSSResource.class);
    getComponentAnnotations(CSSResources.class).stream().map((k) -> k.value()).flatMap(Stream::of)
        .collect(() -> cssResources, (t, u) -> t.add(u), (t, u) -> t.addAll(u));

    List<HTMLResource> htmlResources = getComponentAnnotations(HTMLResource.class);
    getComponentAnnotations(HTMLResources.class).stream().map((k) -> k.value()).flatMap(Stream::of)
        .collect(() -> htmlResources, (t, u) -> t.add(u), (t, u) -> t.addAll(u));

    for (JavaScriptResource js : jsResources) {
      Collection<String> res = js.async() ? resAsyncJavaScript : resJavaScript;
      for (String path : js.value()) {
        register(res, path);
      }
    }

    for (CSSResource css : cssResources) {
      for (String path : css.value()) {
        register(resCSS, path);
      }
    }

    for (HTMLResource html : htmlResources) {
      Collection<String> res = html.target() == Target.HEAD ? resHEAD : resBODY;
      for (String path : html.value()) {
        register(res, path);
      }
    }
  }

  private void register(Collection<String> targetCollection, String path) {
    targetCollection.add(path);
  }

  /**
   * Registers the extension's resources with the {@link AppHTTPServer}.
   *
   * @param server The server instance to work with.
   * @throws IOException on error.
   */
  void init(final AppHTTPServer server) throws IOException {
    serverContextPath = server.getContextPath().replaceFirst("/$", "");
    contextPath = serverContextPath;
    if (extensionPath != null) {
      URL webappUrl = initExtensionResourceURL();
      if (webappUrl != null) {
        WebAppContext wac = server.registerContext(this, extensionPath, webappUrl);
        contextPath = wac.getContextPath().replaceFirst("/$", "");
      }
    }

    htmlHead = this.initHtmlHead();
    htmlBodyTop = this.initHtmlBodyTop();
  }

  /**
   * Returns the resource URL containing the web content for this extension.
   *
   * @return The URL (default to the "webapp/" folder relative to the extension's class name)
   */
  URL initExtensionResourceURL() {
    ResourcePath path = getMostRecentComponentAnnotation(ResourcePath.class);
    if (path != null) {
      return getComponentResource(path.value());
    }

    return getComponentResource("webapp/");
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

  private String initHtmlHead() throws IOException {
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

    for (String path : resHEAD) {
      sb.append(getContentsOfResource(path));
    }

    return sb.toString();
  }

  private String initHtmlBodyTop() throws IOException {
    if (resHEAD.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (String path : resHEAD) {
      sb.append(getContentsOfResource(path));
    }
    return sb.toString();
  }

  private String getContentsOfResource(String path) throws IOException {
    try (InputStream in = getComponentResource("include/noLongerCurrent.html").openStream();
        Scanner sc = new Scanner(in, "UTF-8")) {
      return sc.useDelimiter("\\Z").next();
    }
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
  String htmlHead(final HttpSession context) {
    return htmlHead;
  }

  /**
   * Returns an HTML string that may be added to the top of the HTML BODY section of a web page to
   * initialize this extension.
   *
   * @param context The context of the page.
   * @return The HTML string.
   */
  String htmlBodyTop(final HttpSession context) {
    return htmlBodyTop;
  }

  /**
   * Performs dependency checks.
   *
   * @param app The server app
   * @param extensions The extensions to check.
   * @throws ExtensionDependencyException on dependency conflict.
   */
  void verifyDependencies(final ServerApp app, Set<Class<?>> extensions)
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
