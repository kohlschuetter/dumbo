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

import org.eclipse.jetty.jsp.JettyJspServlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public class CachingJspServlet extends JettyJspServlet {

  private static final long serialVersionUID = 1L;
  private ServletContext context;

  @Override
  public void init() throws ServletException {
    context = getServletContext();
  }

  private boolean checkCache(String path, String generatedPath, HttpServletRequest req,
      HttpServletResponse resp) throws ServletException, IOException {
    if (generatedPath == null || context.getRealPath(path) == null) {
      return false;
    }

    File generatedFile = new File(context.getRealPath(generatedPath));
    if (/* generatedFile.exists() && */ !"true".equals(req.getParameter("reload"))) {
      return false;
    }

    File generatedFileParent = generatedFile.getParentFile();
    if (!generatedFileParent.canWrite()) {
      return false;
    }
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
          try (InputStreamReader in = new InputStreamReader(new FileInputStream(tmpFile),
              StandardCharsets.UTF_8)) {
            in.transferTo(pw);
          }
        }

        if (generate.get()) {
          System.out.println("Generating " + generatedFile);
          if (!tmpFile.renameTo(generatedFile)) {
            System.out.println("FAILED: " + generatedFile);
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

    if (!checkCache(path, generatedPath, req, resp)) {
      super.service(req, resp);
    }
  }
}
