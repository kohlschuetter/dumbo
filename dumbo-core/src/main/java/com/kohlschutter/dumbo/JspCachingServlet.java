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
import java.nio.file.Files;
import java.nio.file.Path;
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

  private transient ServletContext context;

  @Override
  public void init() throws ServletException {
    this.context = getServletContext();
  }

  @SuppressWarnings({"PMD.NcssCount", "PMD.CognitiveComplexity", "PMD.NPathComplexity"})
  private boolean checkCache(String path, String generatedPath, HttpServletRequest req,
      HttpServletResponse resp) throws ServletException, IOException {
    LOG.debug("Check cache {} -> {}", path, generatedPath);

    String realPath = generatedPath == null ? null : context.getRealPath(generatedPath);

    if (generatedPath == null || context.getRealPath(path) == null || path.contains("..")) {
      if (realPath != null) {
        // we're probably running in cached mode: the jsp file is not present but the cached file is
        req.getRequestDispatcher(generatedPath).forward(req, resp);
        return true;
      }
      LOG.warn("Bad path: {}", generatedPath);
      return false;
    }

    boolean isReload = "true".equals(req.getParameter("reload"));

    File generatedFile;
    if (realPath != null) {
      generatedFile = new File(realPath);
    } else {
      if (DumboServerImpl.checkResourceExists(context, path) && !isReload) {
        LOG.debug("Generated file exists, and reload is not true for path {}", path);
        return false;
      }

      Path p = Path.of(generatedPath);
      Path parent = p.getParent();
      if (parent == null) {
        LOG.warn("Cannot get realpath for generatedPath {}", generatedPath);
        return false;
      }
      String contextRealPath = context.getRealPath("/"); // the context root is guaranteed to point
      // to a temporary working directory
      if (contextRealPath == null) {
        LOG.warn("Cannot get realpath for context path");
        return false;
      }
      generatedFile = new File(contextRealPath, p.toString().replaceFirst("^/+", ""));
      Path parentPath = generatedFile.toPath().getParent();
      if (parentPath != null) {
        Files.createDirectories(parentPath);
      }
    }

    if (generatedFile.exists() && !isReload) {
      LOG.debug("Generated file exists, and reload is not true: {}", generatedFile);
      return false;
    }

    File generatedFileParent = generatedFile.getParentFile();
    if (!generatedFileParent.canWrite()) {
      LOG.warn("Cannot write to location: {}", generatedFile);
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
          LOG.info("Generating {}", generatedFile);
          if (!tmpFile.renameTo(generatedFile)) {
            LOG.error("Generating {} failed", generatedFile);
          } else {
            // System.out.println("took " + (System.currentTimeMillis() - time) + "ms");
          }
        }
      } finally {
        Files.deleteIfExists(tmpFile.toPath());
      }
    }
    return true;
  }

  @Override
  public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    String path = req.getServletPath();

    final String generatedPath;
    if (path.endsWith(".html.jsp")) {
      generatedPath = path.substring(0, path.length() - ".jsp".length());
    } else if (path.endsWith(".jsp.js")) {
      generatedPath = path.substring(0, path.length() - ".jsp.js".length()) + ".js";
    } else {
      generatedPath = null;
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
