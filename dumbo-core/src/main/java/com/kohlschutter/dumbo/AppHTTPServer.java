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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response.CompleteListener;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.jetty.AFSocketClientConnector;
import org.newsclub.net.unix.jetty.AFSocketServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.annotations.ServletMapping;
import com.kohlschutter.dumbo.annotations.Servlets;
import com.kohlschutter.dumbo.exceptions.ExtensionDependencyException;
import com.kohlschutter.dumbo.util.DevTools;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * A simple HTTP Server to run demos locally from within the IDE, with JSON-RPC support.
 *
 * See {@code HelloWorldApp} for a simple demo.
 */
public class AppHTTPServer {
  private static final Logger LOG = LoggerFactory.getLogger(AppHTTPServer.class);

  private final Server server;
  private final String contextPath;
  private Thread serverThread;

  private final ContextHandlerCollection contextHandlers;
  private final ServerApp app;
  private final String jsonPath = "/json";

  private final ErrorHandler errorHandler;

  private final Map<String, String> pathsToRegenerate = new HashMap<>();

  private AFUNIXSocketAddress serverUNIXSocketAddress = null;

  private final QueuedThreadPool threadPool;

  private static URL getWebappBaseURL(final ServerApp app) {
    URL u;

    u = app.getApplicationComponentImpl().getComponentResource("webapp/");
    if (u != null) {
      return u;
    }

    // FIXME
    String path = "/" + app.getApplicationClass().getPackage().getName().replace('.', '/')
        + "/webapp/";
    u = app.getApplicationClass().getResource(path);
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
   * @param app The app.
   * @param path The base path for the server, {@code ""} for root.
   * @param webappBaseURL The location of the resources that should be served.
   * @throws IOException on error.
   *
   * @throws ExtensionDependencyException on dependency conflict.
   */
  public AppHTTPServer(final ServerApp app, final String path, final URL webappBaseURL)
      throws IOException {
    this(0, app, path, webappBaseURL);
  }

  public AppHTTPServer(int tcpPort, final ServerApp app, final String path, final URL webappBaseURL)
      throws IOException {
    int port = tcpPort == 0 ? Integer.parseInt(System.getProperty("dumbo.port", "8081")) : tcpPort;

    Objects.requireNonNull(webappBaseURL);
    this.app = app;

    this.errorHandler = new ErrorHandler();
    errorHandler.setShowServlet(false);

    this.threadPool = new QueuedThreadPool();

    this.server = new Server(threadPool);
    SessionIdManager smgr = new DefaultSessionIdManager(server);
    server.setSessionIdManager(smgr);

    this.contextPath = "/" + (path.replaceFirst("^/", "").replaceFirst("/$", ""));
    app.registerCloseable(new Closeable() {
      @Override
      public void close() throws IOException {
        shutdown();
      }
    });

    File webappBase;
    try {
      webappBase = new File(webappBaseURL.toURI());
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
    if (!webappBase.isDirectory()) {
      throw new FileNotFoundException("Not a directory: " + webappBase);
    }

    contextHandlers = new ContextHandlerCollection();

    {
      Resource res = Resource.newResource(webappBaseURL);
      final WebAppContext wac = new WebAppContext(res, contextPath);

      initWebAppContext(app.getApplicationComponentImpl(), wac);

      ServletHolder sh = new ServletHolder(new JabsorbJSONRPCBridgeServlet());
      sh.setInitOrder(0); // initialize right upon start
      wac.addServlet(sh, jsonPath);
      contextHandlers.addHandler(wac);

      scanWebApp(wac.getContextPath(), null, res);
    }

    app.init(this);

    server.setHandler(contextHandlers);
    server.setConnectors(initConnectors(port, server));
  }

  private void scanWebApp(String contextPrefix, String dirPrefix, Resource dir) throws IOException {
    File file = dir.getFile();
    if (file == null || !file.canWrite()) {
      LOG.warn("FIXME Cannot visit file: " + dir);
      return;
    }

    String dirString = dir.toString();
    if (dirPrefix == null) {
      dirPrefix = dirString;
    } else if (!dirString.equals(dirPrefix) && !dirString.startsWith(dirPrefix)) {
      return;
    }

    for (String name : dir.list()) {
      Resource r = dir.getResource(name);
      if (r.isDirectory()) {
        scanWebApp(contextPrefix, dirPrefix, r);
      } else {
        String path = r.toString();
        if (!path.startsWith(dirPrefix)) {
          continue;
        }

        path = path.substring(dirPrefix.length());
        path = contextPrefix + "/" + path;

        String cachedFile;
        if (name.endsWith(".md")) {
          cachedFile = name.substring(0, name.length() - ".md".length()) + ".html";
        } else if (name.endsWith(".html.jsp")) {
          cachedFile = name.substring(0, name.length() - ".jsp".length());
        } else if (name.endsWith(".jsp.js")) {
          cachedFile = name.substring(0, name.length() - ".jsp.js".length()) + ".js";
        } else {
          continue;
        }

        cachedFile = contextPrefix + "/" + cachedFile;

        pathsToRegenerate.put(path, cachedFile);
      }
    }
  }

  /**
   * Registers a web app context/
   *
   * @param contextPrefix The context prefix.
   * @param pathToWebApp The URL pointing to the resources that should be served.
   * @return The context.
   * @throws IOException
   */
  public WebAppContext registerContext(ComponentImpl comp, final String contextPrefix,
      final URL pathToWebApp) throws IOException {
    Resource res = Resource.newResource(pathToWebApp);
    WebAppContext wac = new WebAppContext(res, (contextPath + contextPrefix).replaceAll("//+",
        "/"));
    scanWebApp(wac.getContextPath(), null, res);
    initWebAppContext(comp, wac);
    return registerContext(wac);
  }

  private void initWebAppContext(ComponentImpl comp, WebAppContext wac) throws IOException {
    wac.setDefaultRequestCharacterEncoding("UTF-8");
    wac.setDefaultResponseCharacterEncoding("UTF-8");

    MimeTypes mt = new MimeTypes();
    mt.addMimeMapping("html", MimeTypes.Type.TEXT_HTML_UTF_8.asString());
    mt.addMimeMapping("js", "text/javascript;charset=utf-8");
    mt.addMimeMapping("json", MimeTypes.Type.TEXT_JSON_UTF_8.asString());
    mt.addMimeMapping("txt", MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
    mt.addMimeMapping("xml", MimeTypes.Type.TEXT_XML_UTF_8.asString());
    wac.setMimeTypes(mt);

    wac.setWelcomeFiles(new String[] {"index.html", "index.html.jsp"});
    wac.setErrorHandler(errorHandler);

    wac.setAttribute(AppHTTPServer.class.getName(), this);

    wac.setAttribute("jsonPath", (contextPath + "/" + jsonPath).replaceAll("//+", "/"));

    ServletContext sc = wac.getServletContext();
    sc.setAttribute(ServerApp.class.getName(), app);

    ServletHandler sh = wac.getServletHandler();

    mapServlets(comp, sc, sh);
  }

  private void mapServlets(ComponentImpl comp, ServletContext sc, ServletHandler sh) {
    Map<String, ServletMapping> mappings = new HashMap<>();
    for (Servlets s : comp.getAnnotatedMappingsFromAllReachableComponents(Servlets.class)) {
      for (ServletMapping mapping : s.value()) {
        String mapPath = mapping.map();
        ServletMapping effectiveMapping = mappings.computeIfAbsent(mapPath, (k) -> mapping);
        if (!effectiveMapping.equals(mapping)) {
          throw new IllegalStateException("Conflicting servlet mapping: " + mapPath + " to "
              + effectiveMapping + " vs. " + mapping);
        }
      }
    }

    ServletHolder holderDefaultServlet = sh.addServletWithMapping(DefaultServlet.class.getName(),
        "/");
    holderDefaultServlet.setInitParameter("etags", "true");
    // holderDefaultServlet.setInitParameter("dirAllowed", "false");
    holderDefaultServlet.setInitParameter("useFileMappedBuffer", "true");
    holderDefaultServlet.setInitParameter("stylesheet", AppHTTPServer.class.getResource(
        "/com/kohlschutter/dumbo/appbase/css/jetty-dir.css").toExternalForm());
    sc.setAttribute("holder." + DefaultServlet.class.getName(), holderDefaultServlet);

    for (Map.Entry<String, ServletMapping> en : mappings.entrySet()) {
      String mapPath = en.getKey();
      ServletMapping m = en.getValue();

      Class<? extends Servlet> mapToClass = m.to();

      ServletHolder holder;

      if (mapToClass.getPackage() == AppHTTPServer.class.getPackage()) {
        Servlet servlet;
        try {
          servlet = Objects.requireNonNull(mapToClass.getDeclaredConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
          throw new IllegalStateException(e);
        }
        holder = new ServletHolder(servlet);
        holder.setServletHandler(sh);
      } else {
        holder = new ServletHolder(mapToClass);
      }

      holder.setInitOrder(m.initOrder());
      sc.setAttribute("holder." + mapToClass.getName(), holder);

      sh.addServletWithMapping(holder, mapPath);
    }
  }

  public static ServerApp getServerApp(ServletContext sc) {
    return (ServerApp) sc.getAttribute(ServerApp.class.getName());
  }

  WebAppContext registerContext(WebAppContext wac) {
    contextHandlers.addHandler(wac);
    return wac;
  }

  private HttpClient newServerHttpClient() {
    if (serverUNIXSocketAddress == null) {
      return new HttpClient();
    }

    ClientConnector clientConnector = AFSocketClientConnector.withSocketAddress(
        serverUNIXSocketAddress);
    return new HttpClient(new HttpClientTransportDynamic(clientConnector));
  }

  private CompletableFuture<Void> regeneratePaths() throws Exception {
    if (pathsToRegenerate.isEmpty()) {
      return CompletableFuture.completedFuture((Void) null);
    }

    return CompletableFuture.runAsync(() -> {
      try {
        URI serverURI = server.getURI();
        String serverURIBase = new URI(serverURI.getScheme(), serverURI.getUserInfo(), serverURI
            .getHost(), serverURI.getPort(), null, null, null).toString();
        // ClientConnector clientConnector = new ClientConnector();
        HttpClient client = newServerHttpClient();
        client.start();

        long time = System.currentTimeMillis();
        CountDownLatch cdl = new CountDownLatch(pathsToRegenerate.size());
        client.setMaxConnectionsPerDestination(1);
        try {
          LOG.info("Regenerating " + cdl.getCount() + " paths...");
          for (String path : pathsToRegenerate.keySet()) {
            String uri = serverURIBase + path + "?reload=true";
            try {
              client.newRequest(uri).method(HttpMethod.HEAD).send(new CompleteListener() {

                @Override
                public void onComplete(Result result) {
                  if (result.isFailed()) {
                    LOG.warn("Regeneration failed for path: " + path, result.getFailure());
                  } else if (result.getResponse().getStatus() != HttpServletResponse.SC_OK) {
                    LOG.warn("Regeneration failed with response " + result.getResponse()
                        + " for path: " + path);
                  }
                  cdl.countDown();
                }
              });
            } catch (Throwable t) {
              t.printStackTrace();
            }
          }
          if (!cdl.await(1, TimeUnit.MINUTES)) {
            LOG.warn("Regeneration is taking a long time");
          }
          cdl.await();
          LOG.info("Regeneration  completed after " + (System.currentTimeMillis() - time) + "ms");
        } finally {
          client.stop();
        }
      } catch (Error | RuntimeException e) {
        e.printStackTrace();
        throw e;
      } catch (Exception e) {
        e.printStackTrace();
        throw new IllegalStateException(e);
      }
    });

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
          regeneratePaths();

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
    LOG.warn("Problems at startup/shutdown", e);
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

      server.stop();
      if (join) {
        server.join();
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
  protected Connector[] initConnectors(int tcpPort, Server targetServer) throws IOException {
    ServerConnector tcpConn = initDefaultTCPConnector(tcpPort, targetServer);

    // FIXME directory
    serverUNIXSocketAddress = AFUNIXSocketAddress.of(new File("/tmp/dumbo-" + tcpConn.getPort()
        + ".sock"));

    return new Connector[] {tcpConn, initUnixConnector(targetServer, serverUNIXSocketAddress)};
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
  protected ServerConnector initDefaultTCPConnector(int port, Server targetServer)
      throws IOException {
    ServerConnector connector = new ServerConnector(targetServer, newHttpConnectionFactory());

    connector.setPort(port <= 0 ? 0 : port);
    connector.setReuseAddress(true);
    connector.setReusePort(true);
    connector.setHost(Inet4Address.getLoopbackAddress().getHostAddress());

    return connector;
  }

  /**
   * Returns a Jetty {@link ServerConnector} that listens on the given UNIX socket address.
   *
   * @param targetServer The server this connector is assigned to.
   * @param address The socket address.
   * @return The connector.
   * @throws IOException on error.
   */
  protected Connector initUnixConnector(Server targetServer, AFUNIXSocketAddress address)
      throws IOException {
    Objects.requireNonNull(targetServer);
    Objects.requireNonNull(address);

    AFSocketServerConnector unixConnector = new AFSocketServerConnector(targetServer,
        newHttpConnectionFactory());
    unixConnector.setListenSocketAddress(address);
    unixConnector.setAcceptQueueSize(128);
    unixConnector.setMayStopServerForce(true);

    return unixConnector;
  }

  String getContextPath() {
    return contextPath;
  }

  public URI getURI() {
    return server.getURI();
  }

  void onSessionShutdown(String sessionId, WeakReference<HttpSession> weakSession) {
    // FIXME: implement server shutdown check
  }
}
