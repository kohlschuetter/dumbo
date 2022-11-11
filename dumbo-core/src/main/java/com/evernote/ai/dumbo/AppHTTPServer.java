/**
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
package com.evernote.ai.dumbo;

import java.io.Closeable;
import java.io.IOException;
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

import com.evernote.ai.dumbo.util.DevTools;

/**
 * A simple HTTP Server to run demos locally from within the IDE, with JSON-RPC support.
 * 
 * @see HelloWorldApp for a simple demo
 */
public class AppHTTPServer {
  private static final Logger LOG = Logger.getLogger(AppHTTPServer.class);

  private final Server server;
  private final String path;
  private Thread serverThread;

  private static final URL getWebappBaseURL(final ServerApp app) {
    URL u;

    u = app.getClass().getResource("webapp/");
    if (u != null) {
      return u;
    }

    u = AppHTTPServer.class.getResource("/" + app.getClass().getPackage().getName().replace('.',
        '/') + "/webapp/");
    Objects.requireNonNull(u);

    return u;
  }

  /**
   * Creates a new HTTP server for the given {@link ServerApp} on a free port.
   * 
   * @throws IOException
   * 
   * @throws ExtensionDependencyException
   */
  public AppHTTPServer(final ServerApp app) throws IOException {
    this(app, getWebappBaseURL(app));
  }

  /**
   * Creates a new HTTP server for the given {@link ServerApp} on a free port, using web resources
   * from the given URL path.
   * 
   * @throws ExtensionDependencyException
   */
  public AppHTTPServer(final ServerApp app, final URL webappBaseURL) throws IOException {
    this(app, "", 0, webappBaseURL);
  }

  /**
   * Creates a new HTTP server for the given {@link ServerApp} on a free port, using web resources
   * from the given URL path.
   * 
   * @throws ExtensionDependencyException
   */
  public AppHTTPServer(final ServerApp app, final String path) throws IOException {
    this(app, path, (URL) null);
  }

  /**
   * Creates a new HTTP server for the given {@link ServerApp} on a free port, using web resources
   * from the given URL path.
   * 
   * @throws ExtensionDependencyException
   */
  public AppHTTPServer(final ServerApp app, final String path, final URL webappBaseURL)
      throws IOException {
    this(app, path, 0, webappBaseURL != null ? webappBaseURL : getWebappBaseURL(app));
  }

  private final ContextHandlerCollection contextHandlers;

  private boolean staticMode = false;

  private final ServerApp app;

  /**
   * Creates a new HTTP server for the given {@link ServerApp} on the given port, using web
   * resources from the given URL path.
   * 
   * @throws ExtensionDependencyException
   */
  private AppHTTPServer(final ServerApp app, final String path, final int port,
      final URL webappBaseURL) throws IOException {
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
    }
    for (Extension ext : app.getExtensions()) {
      ext.doInit(this);
    }

    server.setHandler(contextHandlers);

    // from Server constructor -- only open connector after initialization
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(port <= 0 ? 0 : port);
    connector.setHost("127.0.0.1"); // listen on localhost only
    server.setConnectors(new Connector[] {connector});
  }

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
}
