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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.ee10.jsp.JettyJspServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

final class JspCachingServlet extends JettyJspServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(JspCachingServlet.class);

  private ServletContext context;

  @Override
  public void init() throws ServletException {
    this.context = getServletContext();
  }

  private boolean checkCache(String path, String generatedPath, HttpServletRequest req,
      HttpServletResponse resp) throws ServletException, IOException {

    // FIXME use Path / temporary directory instead
    if (generatedPath == null || context.getRealPath(path) == null || path.contains("..")) {
      return false;
    }

    String realPath = context.getRealPath(generatedPath);
    if (realPath == null) {
      LOG.debug("Cannot get realpath for generatedPath {}", generatedPath);
      return false;
    }
    File generatedFile = new File(realPath);
    if ((generatedFile.exists() && !"true".equals(req.getParameter("reload")))) {
      return false;
    }

    File generatedFileParent = generatedFile.getParentFile();
    if (!generatedFileParent.canWrite()) {
      LOG.warn("Cannot write to location: " + generatedFile);
      return false;
    }

    // long time = System.currentTimeMillis();
    File tmpFile = File.createTempFile(".jsp", ".tmp", generatedFileParent);

    AtomicInteger status = new AtomicInteger(HttpServletResponse.SC_OK);
    AtomicBoolean generate = new AtomicBoolean(true);
    CompletableFuture<PrintWriter> netOut = new CompletableFuture<PrintWriter>();
    try (PrintWriter tmpOut = new PrintWriter(tmpFile, StandardCharsets.UTF_8)) {
      HttpServletResponseWrapper respWrapped = new HttpServletResponseWrapper(resp) {

        @Override
        public void setStatus(int sc) {
          if (sc != HttpServletResponse.SC_OK) {
            generate.set(false);
          }
          status.set(sc);
          super.setStatus(sc);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
          netOut.complete(resp.getWriter());
          return tmpOut;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
          generate.set(false);
          super.sendError(sc, msg);
        }

        @Override
        public void sendError(int sc) throws IOException {
          generate.set(false);
          status.set(sc);
          super.sendError(sc);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
          generate.set(false);
          super.sendRedirect(location);
        }

        @Override
        public void addCookie(Cookie cookie) {
          generate.set(false);
          System.out.println("FIXME COOKIE " + cookie);
          super.addCookie(cookie);
        }

        @Override
        public void addDateHeader(String name, long date) {
          generate.set(false);
          System.out.println("FIXME DATE HEADER " + name + " " + date);
          super.addDateHeader(name, date);
        }

        @Override
        public void addHeader(String name, String value) {
          generate.set(false);
          System.out.println("FIXME HEADER " + name + " " + value);
          super.addHeader(name, value);
        }

        @Override
        public void addIntHeader(String name, int value) {
          generate.set(false);
          System.out.println("FIXME INT HEADER " + name + " " + value);
          super.addIntHeader(name, value);
        }
      };

      super.service(req, respWrapped);
    } finally {
      try {
        PrintWriter pw = netOut.getNow(null);
        if (pw != null) {
          if (!"HEAD".equals(req.getMethod())) {
            try (InputStreamReader in = new InputStreamReader(new FileInputStream(tmpFile),
                StandardCharsets.UTF_8)) {
              in.transferTo(pw);
            }
          }
        }

        if (generate.get()) {
          System.out.println("Generating " + generatedFile);
          if (!tmpFile.renameTo(generatedFile)) {
            System.out.println("FAILED: " + generatedFile);
          } else {
            // System.out.println("took " + (System.currentTimeMillis() - time) + "ms");
          }
        }
      } finally {
        tmpFile.delete();
      }
    }
    return true;
  }

  @Override
  public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    String path = req.getServletPath();

    String generatedPath = null;
    if (path.endsWith(".html.jsp")) {
      generatedPath = path.substring(0, path.length() - ".jsp".length());
    } else if (path.endsWith(".jsp.js")) {
      generatedPath = path.substring(0, path.length() - ".jsp.js".length()) + ".js";
    }

    if (generatedPath == null || !checkCache(path, generatedPath, req, resp)) {
      try {
        super.service(req, resp);
      } catch (IOException | ServletException | RuntimeException e) {
        e.printStackTrace();
        throw e;
      }
    }
  }
}
