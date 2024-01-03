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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Redirects to .css.
 *
 * @author Christian Kohlschütter
 */
public final class ScssFilter extends HttpFilter {
  private static final long serialVersionUID = 1L;
  private static final Pattern PAT_SCSS_SUFFIX = Pattern.compile("\\.scss$");

  @Override
  protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
      throws ServletException, IOException {
    if (!check(req, resp)) {
      chain.doFilter(req, resp);
      return;
    }
  }

  private boolean check(HttpServletRequest req, HttpServletResponse resp) throws IOException,
      ServletException {
    String pathInContext = req.getServletPath();
    if (pathInContext == null) {
      return false;
    }

    // boolean source = "true".equals(req.getParameter("source"));
    // if (source) {
    // return false;
    // }

    Matcher m = PAT_SCSS_SUFFIX.matcher(req.getRequestURI());
    if (!m.find()) {
      throw new IllegalStateException("Unexpected");
    }

    String url = m.replaceFirst(".css");
    String qs = req.getQueryString();
    if (qs != null) {
      url += "?" + qs;
    }

    resp.sendRedirect(url);
    return true;
  }
}
