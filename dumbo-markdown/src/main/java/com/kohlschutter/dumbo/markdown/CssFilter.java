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

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.DumboServerImpl;
import com.kohlschutter.dumbo.ServerApp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Checks if the css file needs to be generated from a .scss file.
 *
 * @author Christian Kohlschütter
 */
public final class CssFilter extends HttpFilter {
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(CssFilter.class);

  private transient ServletContext servletContext;
  private transient ServerApp app;
  private transient ScssCompiler sassCompiler;

  @Override
  public void init() throws ServletException {
    this.servletContext = getServletContext();
    this.app = Objects.requireNonNull(DumboServerImpl.getServerApp(servletContext));
    try {
      this.sassCompiler = new ScssCompiler(app);
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void destroy() {
    try {
      sassCompiler.close();
    } catch (IOException e) {
      LOG.error("Error upon destroy", e);
    }
  }

  @Override
  protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
      throws ServletException, IOException {
    if (!checkSass(req, resp)) {
      chain.doFilter(req, resp);
      return;
    }
  }

  private boolean checkSass(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    String pathInContext = req.getServletPath();
    if (pathInContext == null) {
      return false;
    }
    // String path = req.getRequestURI();

    String mimeType = servletContext.getMimeType(pathInContext);
    resp.setCharacterEncoding("UTF-8");
    resp.setContentType(mimeType);

    boolean reload = "true".equals(req.getParameter("reload"));

    URL resource = servletContext.getResource(pathInContext);
    if (resource != null && !reload) {
      // already transformed or existing resource
      return false;
    }

    String scssPathStr = pathInContext.replaceFirst("\\.css$", "\\.scss");
    URL scss = servletContext.getResource(scssPathStr);
    if (scss == null) {
      return false;
    }

    Path scssPath;
    try {
      scssPath = Path.of(scss.toURI());
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }

    long mdFileLength = Files.size(scssPath);
    if (mdFileLength > Integer.MAX_VALUE) {
      // FIXME: use a lower bound
      resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      return true;
    }

    Path generatedCssPath = app.getWebappWorkDir().toPath().resolve(pathInContext.replaceFirst("^/",
        ""));

    sassCompiler.compile(pathInContext, scssPath, generatedCssPath);

    try (BufferedReader br = Files.newBufferedReader(generatedCssPath)) {
      br.transferTo(resp.getWriter());
    }
    return true;
  }
}
