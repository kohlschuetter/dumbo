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

import java.io.IOException;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
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
final class JsFilter extends HttpFilter {
  private static final long serialVersionUID = 1L;
  private static final Pattern PAT_JS = Pattern.compile("\\.js$");

  @Override
  protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
      throws ServletException, IOException {
    ServletContext servletContext = getServletContext();

    String requestURI = req.getRequestURI();
    String pathInContext = requestURI.substring(req.getContextPath().length());

    // FIXME make this configurable
    resp.setHeader("Cache-Control", "max-age=0, no-cache, no-store, must-revalidate");
    resp.setHeader("Pragma", "no-cache");

    if (DumboServerImpl.checkResourceExists(servletContext, pathInContext)) {
      chain.doFilter(req, resp);
      return;
    } else if (DumboServerImpl.checkResourceExists(servletContext, pathInContext + ".jsp")) {
      req.getRequestDispatcher(pathInContext + ".jsp").forward(req, resp);
    } else {
      String path = PAT_JS.matcher(pathInContext).replaceFirst(".jsp.js");
      if (DumboServerImpl.checkResourceExists(servletContext, path)) {
        req.getRequestDispatcher(path).forward(req, resp);
      } else {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    }
  }
}
