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

import java.io.IOException;
import java.util.regex.Pattern;

import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Map .js URLs to .jsp.js (or .js.jsp) where required.
 *
 * This allows to internally use JSP for JavaScript content without exposing it.
 *
 * <p>
 * Example usage (in a {@code .jsp.js} file):
 * </p>
 * <p>
 * <code>
 * const contextPath = '&lt;%@page session="false" contentType="application/javascript" %&gt;&lt;%=
 * application.getContextPath() %&gt;';
 * </code>
 * </p>
 *
 * @author Christian Kohlschütter
 */
final class JspJsServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private ServletContext servletContext;
  private DefaultServlet defaultServlet;
  private JspCachingServlet jspServlet;
  private static final Pattern PAT_JS = Pattern.compile("\\.js$");

  @Override
  public void init() throws ServletException {
    servletContext = getServletContext();
    defaultServlet = (DefaultServlet) ((ServletHolder) getServletContext().getAttribute("holder."
        + DefaultServlet.class.getName())).getServlet();
    jspServlet = (JspCachingServlet) ((ServletHolder) getServletContext().getAttribute("holder."
        + JspCachingServlet.class.getName())).getServlet();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    String requestURI = req.getRequestURI();
    String pathInContext = requestURI.substring(req.getContextPath().length());

    if (AppHTTPServer.checkResourceExists(servletContext, pathInContext)) {
      if (pathInContext.contains(".jsp.js")) {
        jspServlet.service(req, resp);
      } else {
        defaultServlet.service(req, resp);
      }
      return;
    } else if (AppHTTPServer.checkResourceExists(servletContext, pathInContext + ".jsp")) {
      req.getRequestDispatcher(pathInContext + ".jsp").forward(req, resp);
    } else {
      String path = PAT_JS.matcher(pathInContext).replaceFirst(".jsp.js");
      if (AppHTTPServer.checkResourceExists(servletContext, path)) {
        req.getRequestDispatcher(path).forward(req, resp);
      } else {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    }
  }
}
