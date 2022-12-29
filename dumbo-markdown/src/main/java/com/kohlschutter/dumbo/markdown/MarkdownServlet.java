/*
 * dumbo-markdown
 *
 * Copyright 2022 Christian Kohlschütter
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

  private String realPathPrefix;
  private MarkdownSupportImpl mdConfig;

  @Override
  public void init() throws ServletException {
    this.servletContext = getServletContext();
    this.app = Objects.requireNonNull(AppHTTPServer.getServerApp(servletContext));

    this.realPathPrefix = servletContext.getRealPath("") + "/";

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
    } catch (IOException e) {
      // e.printStackTrace();
      throw e;
    }
  }

  protected void doGet0(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {

    String path = servletContext.getRealPath(req.getServletPath());
    if (path == null) {
      return;
    }

    File mdFile = new File(path);
    if (!mdFile.exists() || mdFile.isDirectory()) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    long mdFileLength = mdFile.length();
    if (mdFileLength > Integer.MAX_VALUE) {
      // FIXME: use a lower bound
      resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      return;
    }

    if (!path.startsWith(realPathPrefix)) {
      throw new IllegalStateException("realPath not below " + realPathPrefix + ": " + path);
    }
    String relativePath = path.substring(realPathPrefix.length());

    mdConfig.renderMarkdown(resp, relativePath, mdFile, mdFile, "true".equals(req.getParameter(
        "reload")), null);
  }
}
