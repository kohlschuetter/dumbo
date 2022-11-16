/*
 * Copyright 2022 Christian Kohlschütter
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

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

import com.kohlschutter.dumbo.util.DevTools;

import jakarta.servlet.ServletContext;

/**
 * A simple HTTP Server to run demos locally from within the IDE, with JSON-RPC support.
 *
 * See {@code HelloWorldApp} for a simple demo.
 */
public class AppHTTPServer {
  private static final Logger LOG = Logger.getLogger(AppHTTPServer.class);

  private final Server server;
  private final String contextPath;
  private Thread serverThread;

  private final ContextHandlerCollection contextHandlers;
  private final ServerAppBase app;
  private final String jsonPath = "/json";

  private final ErrorHandler errorHandler;

  private static URL getWebappBaseURL(final ServerAppBase app) {
    URL u;

    u = app.getClass().getResource("webapp/");
    if (u != null) {
      return u;
    }

    String path = "/" + app.getClass().getPackage().getName().replace('.', '/') + "/webapp/";
    u = app.getClass().getResource(path);
    Objects.requireNonNull(u, () -> {
      return "Resource path is missing: " + path;
    });

    return u;
  }

  /**
   * Creates a new HTTP server for the given {@link ServerAppBase} on a free port.
   *
   * @param app The server app.
   * @throws IOException on error
   * @throws ExtensionDependencyException on dependency conflict.
   */
  public AppHTTPServer(final ServerAppBase app) throws IOException {
    this(app, getWebappBaseURL(app));
  }

  /**
   * Creates a new HTTP server for the given {@link ServerAppBase} on a free port, using web
   * resources from the given URL path.
   *
   * @param app The server app.
   * @param webappBaseURL The location of the resources that should be served.
   *
   * @throws ExtensionDependencyException on dependency conflict.
   */
  public AppHTTPServer(final ServerAppBase app, final URL webappBaseURL) throws IOException {
    this(app, "", webappBaseURL);
  }

  /**
   * Creates a new HTTP server for the given {@link ServerAppBase}, using web resources from the
   * given URL path.
   *
   * @param app The server app.
   * @param path The base path for the server, {@code ""} for root.
   *
   * @throws ExtensionDependencyException on error
   */
  public AppHTTPServer(final ServerAppBase app, final String path) throws IOException {
    this(app, path, (URL) null);
  }

  /**
   * Creates a new HTTP server for the given {@link ServerAppBase}, using web resources from the
   * given URL path.
   *
   * @param app The app.
   * @param path The base path for the server, {@code ""} for root.
   * @param webappBaseURL The location of the resources that should be served.
   * @throws IOException on error.
   *
   * @throws ExtensionDependencyException on dependency conflict.
   */
  public AppHTTPServer(final ServerAppBase app, final String path, final URL webappBaseURL)
      throws IOException {
    this.app = app;

    this.errorHandler = new ErrorHandler();
    errorHandler.setShowServlet(false);

    this.server = new Server();
    this.contextPath = "/" + (path.replaceFirst("^/", "").replaceFirst("/$", ""));
    app.registerCloseable(new Closeable() {
      @Override
      public void close() throws IOException {
        shutdown();
      }
    });

    app.initInternal();

    contextHandlers = new ContextHandlerCollection();
    {
      final WebAppContext wac = new WebAppContext(webappBaseURL.toExternalForm(), contextPath);
      wac.setInitParameter("etags", "true");
      initWebAppContext(wac);

      wac.addServlet(JabsorbJSONRPCBridgeServlet.class, jsonPath);
      contextHandlers.addHandler(wac);

      // wac.getSessionHandler().addEventListener(new HttpSessionListener() {
      //
      // @Override
      // public void sessionCreated(HttpSessionEvent se) {
      // }
      //
      // @Override
      // public void sessionDestroyed(HttpSessionEvent se) {
      // }
      // });
    }
    for (Extension ext : app.getExtensions()) {
      ext.doInit(this);
    }

    server.setHandler(contextHandlers);
    server.setConnectors(initConnectors(server));
  }

  /**
   * Registers a web app context/
   *
   * @param contextPrefix The context prefix.
   * @param pathToWebApp The URL pointing to the resources that should be served.
   * @return The context.
   * @throws IOException
   */
  public WebAppContext registerContext(final String contextPrefix, final URL pathToWebApp)
      throws IOException {
    WebAppContext wac = new WebAppContext(pathToWebApp.toExternalForm(), (contextPath
        + contextPrefix).replaceAll("//+", "/"));
    initWebAppContext(wac);
    return registerContext(wac);
  }

  private void initWebAppContext(WebAppContext wac) throws IOException {
    wac.setWelcomeFiles(new String[] {"index.html.jsp", "index.html"});
    wac.setErrorHandler(errorHandler);

    wac.setAttribute("path." + JabsorbJSONRPCBridgeServlet.class.getName(), contextPath + jsonPath);
    wac.setAttribute("jsonPath", (contextPath + "/" + jsonPath).replaceAll("//+", "/"));

    ServletContext sc = wac.getServletContext();
    sc.setAttribute(ServerApp.class.getName(), app);

    ServletHolder dsh = wac.addServlet(DefaultServlet.class.getName(), "/");
    sc.setAttribute("holder." + DefaultServlet.class.getName(), dsh);

    ServletHolder jsh = wac.addServlet(JettyJspServlet.class.getName(), "*.jsp");
    sc.setAttribute("holder." + JettyJspServlet.class.getName(), jsh);

    dsh.setInitParameter("useFileMappedBuffer", "true");
    dsh.setInitParameter("stylesheet", AppHTTPServer.class.getResource(
        "/com/kohlschutter/dumbo/appbase/css/jetty-dir.css").toExternalForm());

    ServletHandler sh = wac.getServletHandler();

    sh.addServletWithMapping(HtmlJspServlet.class, "*.html");
    sh.addServletWithMapping(JspJsServlet.class, "*.js");

    // sh.setInitParameter("dirAllowed", "false");
  }

  WebAppContext registerContext(WebAppContext wac) {
    contextHandlers.addHandler(wac);
    return wac;
  }

  /**
   * Starts the HTTP Server and runs it in a separate thread.
   */
  public void start() {
    if (serverThread != null) {
      return;
    }
    serverThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          server.start();

          DevTools.init();

          onServerStart();
          server.join();
          LOG.info("Shutting down ...");
          onServerStop();
        } catch (Exception e) {
          onServerException(e);
        }
      }
    });
    serverThread.start();
  }

  /**
   * Starts the HTTP Server, runs it in a separate thread. The call will only return when the server
   * has been shut down.
   */
  public void startAndWait() {
    start();
    try {
      serverThread.join();
    } catch (InterruptedException e) {
      onServerException(e);
    }
  }

  /**
   * This method is called upon server start.
   */
  protected void onServerStart() {
  }

  /**
   * This method is called upon server stop.
   */
  protected void onServerStop() {
    // Make sure the JVM exits -- Maven's exec:java may spawn extra threads...
    new Thread() {
      {
        setDaemon(true);
      }

      @Override
      public void run() {
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        System.exit(0);
      }
    }.start();
  }

  /**
   * This method is called upon any exception during server startup or shutdown
   *
   * @param e The Exception
   */
  protected void onServerException(final Exception e) {
    LOG.warn("Problems at startup/shutdown: " + e, e);
  }

  /**
   * Instructs the HTTP server to stop, and wait until the server has been shut down.
   */
  public void shutdown() {
    stop(true);
  }

  private void stop(final boolean join) {
    if (serverThread == null) {
      return;
    }
    try {
      server.setStopTimeout(0);

      // This silences QueuedThreadPool's whining
      final Logger qtpLogger = Logger.getLogger(QueuedThreadPool.class);
      final Level qtpLogLevel = qtpLogger.getLevel();

      try {
        qtpLogger.setLevel(Level.FATAL);
        server.stop();
        if (join) {
          server.join();
        }
      } finally {
        qtpLogger.setLevel(qtpLogLevel);
      }
      serverThread.interrupt();
      serverThread = null;
    } catch (Exception e) {
      onServerException(e);
    }
  }

  /**
   * Returns an array of {@link Connector}s to be used for the given server.
   *
   * @param targetServer The target server.
   * @return The connector(s).
   * @throws IOException on error.
   */
  protected Connector[] initConnectors(Server targetServer) throws IOException {
    return new Connector[] {initDefaultTCPConnector(targetServer)};
  }

  protected HttpConnectionFactory newHttpConnectionFactory() {
    HttpConfiguration config = new HttpConfiguration();
    config.setSendServerVersion(false);
    return new HttpConnectionFactory(config);
  }

  /**
   * Returns a Jetty {@link ServerConnector} that listens by default on localhost TCP port 8084.
   *
   * The port can be configured using the {@code dumbo.port} system property.
   *
   * @param targetServer The server this connector is assigned to.
   * @return The connector.
   * @throws IOException on error.
   */
  protected ServerConnector initDefaultTCPConnector(Server targetServer) throws IOException {
    ServerConnector connector = new ServerConnector(targetServer, newHttpConnectionFactory());

    int port = Integer.parseInt(System.getProperty("dumbo.port", "8084"));

    connector.setPort(port <= 0 ? 0 : port);
    connector.setReuseAddress(true);
    connector.setReusePort(true);
    connector.setHost(Inet4Address.getLoopbackAddress().getHostAddress());

    return connector;
  }

  public String getContextPath() {
    return contextPath;
  }

  public URI getURI() {
    return server.getURI();
  }
}
