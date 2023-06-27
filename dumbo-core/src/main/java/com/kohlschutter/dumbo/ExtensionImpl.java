/*
 * Copyright 2022,2023 Christian Kohlschütter
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.annotations.CSSResource;
import com.kohlschutter.dumbo.annotations.CSSResources;
import com.kohlschutter.dumbo.annotations.HTMLResource;
import com.kohlschutter.dumbo.annotations.HTMLResource.Target;
import com.kohlschutter.dumbo.annotations.HTMLResources;
import com.kohlschutter.dumbo.annotations.JavaScriptResource;
import com.kohlschutter.dumbo.annotations.JavaScriptResources;
import com.kohlschutter.dumbo.api.DumboComponent;
import com.kohlschutter.dumbo.util.NameObfuscator;
import com.kohlschutter.stringhold.StringHolder;
import com.kohlschutter.stringhold.StringHolderSequence;

/**
 * Provides support for HTML/CSS/JavaScript-based extensions that simplify app development.
 */
final class ExtensionImpl extends ComponentImpl {
  private static final Logger LOG = LoggerFactory.getLogger(ExtensionImpl.class);

  private final String extensionPath;
  private String serverContextPath;
  private String contextPath;

  private StringHolderSequence htmlHead = null;
  private StringHolderSequence htmlBodyTop = null;

  private List<JavaScriptResource> jsResources;
  private List<CSSResource> cssResources;
  private List<HTMLResource> htmlResources;

  ExtensionImpl(Class<? extends DumboComponent> comp, boolean isAppComponent) {
    super(comp);
    this.extensionPath = initPath(isAppComponent);
  }

  private String initPath(boolean isAppComponent) {
    String path = initExtensionPath(isAppComponent);
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

  String initExtensionPath(boolean isAppComponent) {
    ServletContextPath path = getMostRecentComponentAnnotation(ServletContextPath.class);
    if (path != null) {
      return path.value();
    }

    if (isAppComponent) {
      return "/";
    }
    String name = getComponentClass().getName();

    return "/app_/" + NameObfuscator.obfuscate(name);
  }

  /**
   * Called by the app to initialize the {@link ExtensionImpl} for the given {@link AppHTTPServer}.
   *
   * @throws IOException on error.
   */
  @Override
  void initComponent(AppHTTPServer app) throws IOException {
    super.initComponent(app);
    initResources();
    initExtension(app);
  }

  /**
   * Called to register any JavaScript and/or CSS resources that should be used by this app.
   *
   * @see #registerCSS(String)
   * @see #registerJavaScript(String)
   */
  private void initResources() {
    this.jsResources = getComponentAnnotations(JavaScriptResource.class);
    getComponentAnnotations(JavaScriptResources.class).stream().map((k) -> k.value()).flatMap(
        Stream::of).collect(() -> jsResources, (t, u) -> t.add(u), (t, u) -> t.addAll(u));

    this.cssResources = getComponentAnnotations(CSSResource.class);
    getComponentAnnotations(CSSResources.class).stream().map((k) -> k.value()).flatMap(Stream::of)
        .collect(() -> cssResources, (t, u) -> t.add(u), (t, u) -> t.addAll(u));

    this.htmlResources = getComponentAnnotations(HTMLResource.class);
    getComponentAnnotations(HTMLResources.class).stream().map((k) -> k.value()).flatMap(Stream::of)
        .collect(() -> htmlResources, (t, u) -> t.add(u), (t, u) -> t.addAll(u));
  }

  /**
   * Registers the extension's resources with the {@link AppHTTPServer}.
   *
   * @param server The server instance to work with.
   * @throws IOException on error.
   */
  void initExtension(final AppHTTPServer server) throws IOException {
    serverContextPath = server.getContextPath().replaceFirst("/$", "");
    contextPath = serverContextPath;
    if (extensionPath != null) {
      URL webappUrl = initExtensionResourceURL();
      if (webappUrl != null) {
        WebAppContext wac = server.registerContext(this, extensionPath, webappUrl);
        contextPath = wac.getContextPath().replaceFirst("/$", "");
      }
    }

    htmlHead = this.initHtmlHead(server);
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

  private boolean isRelativePath(String path) {
    return !path.startsWith("/") && !path.contains("://");
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

  private StringHolderSequence initHtmlHead(AppHTTPServer server) throws IOException {
    StringHolderSequence sb = new StringHolderSequence();

    final Predicate<StringHolder> optionalInclude = (sh) -> {
      RenderState rs = RenderState.get();
      boolean used = rs.isMarkedUsed(getComponentClass());
      return used;
    };

    for (CSSResource css : cssResources) {
      for (String path : css.value()) {
        String url = toAbsolutePath(path);

        if (isRelativePath(url) && !server.checkResourceExists(url)) {
          // CSS resource doesn't exist, and can be optimized away
          if (css.optional()) {
            LOG.info("Skipping CSS resource " + url + "; missing from " + this);
            continue;
          } else {
            LOG.warn("CSS resource " + url + " is missing from " + this);
          }
        }

        CharSequence s = "<link rel=\"stylesheet\" href=\"" + xmlEntities(url) + "\" />\n";
        if (css.optional()) {
          s = StringHolder.withConditionalStringHolder(StringHolder.withContent(s),
              optionalInclude);
        }

        sb.append(s);
      }
    }

    for (JavaScriptResource js : jsResources) {
      String defer = js.defer() ? " defer=\"defer\"" : "";
      String async = js.async() ? " async=\"async\"" : "";

      for (String path : js.value()) {
        String url = toAbsolutePath(path);

        if (isRelativePath(url) && !server.checkResourceExists(url)) {
          // JavaScript resource doesn't exist, and can be optimized away
          if (js.optional()) {
            LOG.info("Skipping JavaScript resource " + url + "; missing from " + this);
            continue;
          } else {
            LOG.warn("JavaScript" + url + " is missing from " + this);
          }
        }

        CharSequence s = "<script type=\"text/javascript\" src=\"" + xmlEntities(url) + "\"" + defer
            + async + "></script>\n";
        if (js.optional()) {
          s = StringHolder.withConditionalStringHolder(StringHolder.withContent(s),
              optionalInclude);
        }

        sb.append(s);
      }
    }

    for (HTMLResource html : htmlResources) {
      if (html.target() != Target.HEAD) {
        continue;
      }
      for (String path : html.value()) {
        sb.append(getContentsOfResource(path));
      }
    }

    return sb;
  }

  private StringHolderSequence initHtmlBodyTop() throws IOException {
    StringHolderSequence sb = new StringHolderSequence();

    for (HTMLResource html : htmlResources) {
      if (html.target() != Target.BODY) {
        continue;
      }
      for (String path : html.value()) {
        sb.append(getContentsOfResource(path));
      }
    }

    return sb;
  }

  private String getContentsOfResource(String path) throws IOException {
    try (InputStream in = getComponentResource(path).openStream();
        Scanner sc = new Scanner(in, "UTF-8")) {
      return sc.useDelimiter("\\Z").next();
    } catch (NullPointerException e) {
      // FIXME This is Eclipse messing with us (build path entries are missing)
      throw (FileNotFoundException) new FileNotFoundException("Cannot get contents of resource: "
          + path + "; relative to " + getComponentClass()).initCause(e);
    }
  }

  /**
   * Returns an HTML string that may be added to the HTML HEAD section of a web page to initialize
   * this extension.
   *
   * By default, this returns a set of {@code <LINK>} and/or {@code SCRIPT} HTML elements, pointing
   * to the resources registered in {@link #initResources()}.
   *
   * @param app The server app.
   * @return The HTML string.
   */
  StringHolderSequence htmlHead(final ServerApp app) {
    return htmlHead.clone();
  }

  /**
   * Returns an HTML string that may be added to the top of the HTML BODY section of a web page to
   * initialize this extension.
   *
   * @param app The server app.
   * @return The HTML string.
   */
  StringHolderSequence htmlBodyTop(final ServerApp app) {
    return htmlBodyTop.clone();
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
