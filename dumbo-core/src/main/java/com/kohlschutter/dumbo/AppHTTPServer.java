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
import java.net.URL;
import java.util.Objects;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

import com.kohlschutter.dumbo.util.DevTools;

/**
 * A simple HTTP Server to run demos locally from within the IDE, with JSON-RPC support.
 *
 * See {@code HelloWorldApp} for a simple demo.
 */
public class AppHTTPServer {
  private static final Logger LOG = Logger.getLogger(AppHTTPServer.class);

  private final Server server;
  private final String path;
  private Thread serverThread;

  private final ContextHandlerCollection contextHandlers;
  private boolean staticMode = false;
  private final ServerApp app;

  private static URL getWebappBaseURL(final ServerApp app) {
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
   * Creates a new HTTP server for the given {@link ServerApp} on a free port.
   *
   * @param app The server app.
   * @throws IOException on error
   * @throws ExtensionDependencyException on dependency conflict.
   */
  public AppHTTPServer(final ServerApp app) throws IOException {
    this(app, getWebappBaseURL(app));
  }

  /**
   * Creates a new HTTP server for the given {@link ServerApp} on a free port, using web resources
   * from the given URL path.
   *
   * @param app The server app.
   * @param webappBaseURL The location of the resources that should be served.
   *
   * @throws ExtensionDependencyException on dependency conflict.
   */
  public AppHTTPServer(final ServerApp app, final URL webappBaseURL) throws IOException {
    this(app, "", webappBaseURL);
  }

  /**
   * Creates a new HTTP server for the given {@link ServerApp}, using web resources from the given
   * URL path.
   *
   * @param app The server app.
   * @param path The base path for the server, {@code ""} for root.
   *
   * @throws ExtensionDependencyException on error
   */
  public AppHTTPServer(final ServerApp app, final String path) throws IOException {
    this(app, path, (URL) null);
  }

  /**
   * Creates a new HTTP server for the given {@link ServerApp}, using web resources from the given
   * URL path.
   *
   * @param app The app.
   * @param path The base path for the server, {@code ""} for root.
   * @param webappBaseURL The location of the resources that should be served.
   *
   * @throws ExtensionDependencyException on dependency conflict.
   */
  public AppHTTPServer(final ServerApp app, final String path, final URL webappBaseURL)
      throws IOException {
    this.app = app;
    this.server = new Server();
    this.path = path.replaceFirst("^/", "");
    app.registerCloseable(new Closeable() {
      @Override
      public void close() throws IOException {
        shutdown();
      }
    });

    app.initInternal();

    contextHandlers = new ContextHandlerCollection();
    {
      final WebAppContext wac = new WebAppContext(webappBaseURL.toExternalForm(), "");
      wac.getServletContext().setAttribute("app", app);
      wac.addServlet(JabsorbJSONRPCBridgeServlet.class, "/json");
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
   */
  public WebAppContext registerContext(final String contextPrefix, final URL pathToWebApp) {
    return registerContext(new WebAppContext(pathToWebApp.toExternalForm(), contextPrefix));
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

          staticMode = DevTools.isShiftPressed();
          if (staticMode) {
            LOG.info("Shift press detected -- enabling static design mode.");
          } else {
            LOG.info("Running in live mode. Start server with \"shift\" key pressed to "
                + "enable static design mode.");
          }
          app.setStaticDesignMode(staticMode);

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
   *
   * By default, it tries to open the server's root page in a browser window.
   */
  protected void onServerStart() {
    try {
      String url = server.getURI().toString() + path;

      if (isStaticMode()) {
        url += "?static";
      }

      LOG.info("Opening page in browser: " + url);
      DevTools.openURL(url);
    } catch (Exception e) {
      // ignore
    }
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
   * Checks whether this server is running in "static" mode.
   *
   * @return {@code true} if in static mode.
   */
  public boolean isStaticMode() {
    return staticMode;
  }

  protected Connector[] initConnectors(Server targetServer) throws IOException {
    ServerConnector localhostConnector = new ServerConnector(targetServer);

    int port = Integer.parseInt(System.getProperty("dumbo.port", "8084"));

    localhostConnector.setPort(port <= 0 ? 0 : port);
    localhostConnector.setReuseAddress(true);
    localhostConnector.setReusePort(true);
    localhostConnector.setHost(Inet4Address.getLoopbackAddress().getHostAddress());

    return new Connector[] {localhostConnector};
  }
}
