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

import com.kohlschutter.dumbo.AppHTTPServer;
import com.kohlschutter.dumbo.ServerApp;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MarkdownServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private ServletContext servletContext;

  private ServerApp app;

  private MarkdownSupportImpl mdConfig;

  @Override
  public void init() throws ServletException {
    this.servletContext = getServletContext();
    this.app = Objects.requireNonNull(AppHTTPServer.getServerApp(servletContext));

    try {
      mdConfig = app.getImplementationByIdentity(MarkdownSupportImpl.COMPONENT_IDENTITY,
          () -> new MarkdownSupportImpl(app));
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    try {
      doGet0(req, resp);
    } catch (ServletException | IOException | RuntimeException e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected void doGet0(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    String servletPath = req.getServletPath();

    URL url = servletContext.getResource(servletPath);
    if (servletPath == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if (url == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    Path mdPath;
    try {
      mdPath = Path.of(url.toURI());
    } catch (URISyntaxException e1) {
      e1.printStackTrace();
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if (!Files.exists(mdPath) || Files.isDirectory(mdPath)) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    long mdFileLength = Files.size(mdPath);
    if (mdFileLength > Integer.MAX_VALUE) {
      // FIXME: use a lower bound
      resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      return;
    }

    String relativePath = servletPath;
    if (relativePath.startsWith("/")) {
      relativePath = relativePath.substring(1);
    }
    File mdFile;
    try {
      mdFile = mdPath.toFile();
    } catch (Exception e) {
      System.err.println("Cannot convert path " + mdPath + " to file: " + e);
      mdFile = new File(app.getWebappWorkDir(), relativePath);
      // System.out.println("using " + mdFile);
    }

    mdConfig.renderMarkdown(relativePath, mdPath, mdFile, "true".equals(req.getParameter(
        "reload")), resp, null, null);
  }
}
