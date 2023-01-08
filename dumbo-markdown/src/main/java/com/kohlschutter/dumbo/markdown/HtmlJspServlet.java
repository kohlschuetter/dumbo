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

import java.io.IOException;

import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

import com.kohlschutter.dumbo.AppHTTPServer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Map .html URLs to .html.jsp, .md where required.
 *
 * This allows to internally use JSP without exposing it.
 *
 * @author Christian Kohlschütter
 */
public class HtmlJspServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private DefaultServlet defaultServlet;
  private ServletContext servletContext;
  private static final String[] suffixesWithoutHtml = new String[] {".jsp", ".md"};
  private static final String[] suffixesWithHtml = new String[] {".md", ".html.jsp"};

  @Override
  public void init() throws ServletException {
    this.servletContext = getServletContext();
    defaultServlet = (DefaultServlet) ((ServletHolder) servletContext.getAttribute("holder."
        + DefaultServlet.class.getName())).getServlet();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    String requestURI = req.getRequestURI();
    String pathInContext = requestURI.substring(req.getContextPath().length());

    if (AppHTTPServer.checkResourceExists(servletContext, pathInContext)) {
      // *.html file exists -> use DefaultServlet
      defaultServlet.service(req, resp);
      return;
    }

    String[] suffixes;
    if (pathInContext.endsWith(".html")) {
      pathInContext = pathInContext.substring(0, pathInContext.length() - ".html".length());
      suffixes = suffixesWithHtml;
    } else {
      // unexpected
      suffixes = suffixesWithoutHtml;
    }
    for (String suffix : suffixes) {
      String path = pathInContext + suffix;
      if (AppHTTPServer.checkResourceExists(servletContext, path)) {
        req.getRequestDispatcher(path).forward(req, resp);
        return;
      }
    }

    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
  }
}
