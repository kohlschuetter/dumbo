/*
 * dumbo-markdown
 *
 * Copyright 2022,2023 Christian Kohlschütter
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
package com.kohlschutter.dumbo.markdown;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.kohlschutter.dumbo.DumboServerImpl;
import com.kohlschutter.dumbo.ServerApp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Add support for liquid template files.
 *
 * @author Christian Kohlschütter
 */
public final class LiquidFilter extends HttpFilter {
  private static final long serialVersionUID = 1L;

  private transient MarkdownSupportImpl mdSupport;
  private ServletContext servletContext;
  private ServerApp app;

  @Override
  public void init() throws ServletException {
    this.servletContext = getServletContext();
    this.app = Objects.requireNonNull(DumboServerImpl.getServerApp(servletContext));

    try {
      mdSupport = app.getImplementationByIdentity(MarkdownSupportImpl.COMPONENT_IDENTITY,
          () -> new MarkdownSupportImpl(app));
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
      throws ServletException, IOException {
    if (!checkLiquid(req, resp, chain)) {
      chain.doFilter(req, resp);
      return;
    }
  }

  private boolean checkLiquid(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
      throws IOException {

    String pathInContext = req.getRequestURI().substring(req.getContextPath().length());

    URL resource = servletContext.getResource(pathInContext);
    if (resource == null) {
      return false;
    }

    Path path;
    try {
      path = Path.of(resource.toURI());
    } catch (URISyntaxException e) {
      return false;
    }

    if (path.startsWith(app.getWebappWorkDir().toPath())) {
      // already transformed
      return false;
    }

    String servletPath = req.getServletPath();
    if (servletPath == null) {
      return false;
    }

    URL url = servletContext.getResource(servletPath);
    if (url == null) {
      return false;
    }

    Path mdPath;
    try {
      mdPath = Path.of(url.toURI());
    } catch (URISyntaxException e) {
      return false;
    }

    if (!Files.exists(mdPath) || Files.isDirectory(mdPath)) {
      return false;
    }

    long mdFileLength = Files.size(mdPath);
    if (mdFileLength > Integer.MAX_VALUE) {
      // FIXME: use a lower bound
      resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      return true;
    }

    String relativePath = servletPath;
    if (relativePath.startsWith("/")) {
      relativePath = relativePath.substring(1);
    }
    File mdFile = new File(app.getWebappWorkDir(), relativePath);

    boolean reload = "true".equals(req.getParameter("reload"));

    String mimeType = servletContext.getMimeType(servletPath);
    resp.setContentType(mimeType);

    mdSupport.render(false, relativePath, mdPath, mdFile, reload, resp, null, null, null);
    return true;
  }
}
