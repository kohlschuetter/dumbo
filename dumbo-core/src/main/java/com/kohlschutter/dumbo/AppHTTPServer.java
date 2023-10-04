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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Response.CompleteListener;
import org.eclipse.jetty.client.Result;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.ee10.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.resource.CombinedResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.jetty.AFSocketClientConnector;
import org.newsclub.net.unix.jetty.AFSocketServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.annotations.FilterMapping;
import com.kohlschutter.dumbo.annotations.Filters;
import com.kohlschutter.dumbo.annotations.ServletMapping;
import com.kohlschutter.dumbo.annotations.Servlets;
import com.kohlschutter.dumbo.exceptions.ExtensionDependencyException;
import com.kohlschutter.dumbo.util.DevTools;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
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

  private final LinkedHashMap<WebAppContext, ContextMetadata> contexts = new LinkedHashMap<>();

  private static URL getWebappBaseURL(final ServerApp app) {
    URL u;

    u = app.getApplicationExtensionImpl().getComponentResource("webapp/");
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

    Objects.requireNonNull(webappBaseURL, "Webapp baseURL not specified or resources not found");
    this.app = app;

    this.errorHandler = new ErrorHandler();
    errorHandler.setShowServlet(false);

    this.threadPool = new QueuedThreadPool();

    this.server = new Server(threadPool);
    // server.setDumpAfterStart(true); // for debugging

    this.contextPath = "/" + (path.replaceFirst("^/", "").replaceFirst("/$", ""));
    app.registerCloseable(new Closeable() {
      @Override
      public void close() throws IOException {
        shutdown();
      }
    });

    contextHandlers = new ContextHandlerCollection();

    app.init(this, path, webappBaseURL);

    URI webappBaseURI;
    try {
      webappBaseURI = webappBaseURL.toURI();
    } catch (URISyntaxException e1) {
      throw new IllegalStateException(e1);
    }

    {
      Resource res;
      try {
        res = ResourceFactory.root().newResource(List.of(app.getWebappWorkDir().toURI(),
            webappBaseURL.toURI()));
      } catch (URISyntaxException e) {
        throw new IllegalStateException(e);
      }
      final WebAppContext wac = new WebAppContext(res, contextPath);
      wac.setBaseResource(res);

      wac.setLogger(LOG);
      wac.addServletContainerInitializer(new JettyJasperInitializer());

      wac.setTempDirectory(new File(app.getWorkDir(), "jetty.tmp"));
      wac.setTempDirectoryPersistent(true);

      initWebAppContext(app.getApplicationExtensionImpl(), wac);

      ServletHolder sh = new ServletHolder(new JabsorbJSONRPCBridgeServlet());
      sh.setInitOrder(0); // initialize right upon start
      wac.addServlet(sh, jsonPath);

      registerContext(wac, webappBaseURI);

      scanWebApp(wac.getContextPath(), null, res);

      wac.setServer(server);
    }

    app.initComponents(this);

    server.setHandler(contextHandlers);
    server.setConnectors(initConnectors(port, server));
  }

  private final Set<URI> scannedFiles = new HashSet<>();

  private static String normalizeFileSlashes(String uri) {
    return uri.replace("file:///", "file:/");
  }

  /**
   * Scan the webapp's resources for files that we should request via HTTP (to trigger caching,
   * etc.).
   *
   * @param contextPrefix The context prefix.
   * @param dirPrefixes Valid directory prefixes.
   * @param dir The directory resource.
   * @throws IOException on error.
   */
  private void scanWebApp(String contextPrefix, String[] dirPrefixes, Resource dir)
      throws IOException {
    URI key = dir.getURI();
    if (!scannedFiles.add(key)) {
      return;
    }

    if (dirPrefixes == null) {
      if (dir instanceof CombinedResource) {
        CombinedResource cr = (CombinedResource) dir;
        List<String> prefixes = new ArrayList<>();
        for (Resource r : cr.getResources()) {
          prefixes.add(normalizeFileSlashes(r.getURI().toString()));
        }
        dirPrefixes = prefixes.toArray(new String[0]);
      } else {
        dirPrefixes = new String[] {normalizeFileSlashes(dir.getURI().toString())};
      }
    } else {
      String dirString = dir.getURI().toString();
      boolean ok = false;
      for (String prefix : dirPrefixes) {
        if (dirString.equals(prefix) || dirString.startsWith(prefix)) {
          ok = true;
          break;
        }
      }
      if (!ok) {
        return;
      }
    }

    for (Resource r : dir.list()) {
      if (r.isDirectory()) {
        scanWebApp(contextPrefix, dirPrefixes, r);
      } else {
        String name = r.getName();
        String path = normalizeFileSlashes(r.getURI().toString());

        String okPrefix = null;
        for (String prefix : dirPrefixes) {
          if (path.startsWith(prefix)) {
            okPrefix = prefix;
            break;
          }
        }
        if (okPrefix == null) {
          continue;
        }

        path = path.substring(okPrefix.length());
        path = contextPrefix + "/" + path;

        String cachedFile = processFileName(name);
        if (cachedFile != null && !cachedFile.equals(name)) {
          cachedFile = contextPrefix + "/" + cachedFile;
          pathsToRegenerate.put(path, cachedFile);
        }
      }
    }
  }

  private static String processFileName(String name) {
    if (name.endsWith(".md")) {
      return name.substring(0, name.length() - ".md".length()) + ".html";
    } else if (name.endsWith(".html.jsp")) {
      return name.substring(0, name.length() - ".jsp".length());
    } else if (name.endsWith(".jsp.js")) {
      return name.substring(0, name.length() - ".jsp.js".length()) + ".js";
    } else if (name.endsWith("~")) {
      return null;
    } else if (name.startsWith(".")) {
      // FIXME if desired, add exceptions to forcibly include hidden files
      return null;
    } else {
      return name;
    }
  }

  /**
   * Registers a web app context.
   *
   * @param contextPrefix The context prefix.
   * @param pathToWebAppURL The URL pointing to the resources that should be served.
   * @return The context.
   * @throws IOException
   */
  public WebAppContext registerContext(ComponentImpl comp, final String contextPrefix,
      final URL pathToWebAppURL) throws IOException {
    URI resourceBaseUri;
    try {
      resourceBaseUri = pathToWebAppURL.toURI();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
    Resource res = ResourceFactory.root().newResource(resourceBaseUri);

    String prefix = (contextPath + contextPrefix).replaceAll("//+", "/");
    WebAppContext wac = new WebAppContext(res, prefix);
    wac.setBaseResource(res);
    wac.setLogger(LOG);
    wac.addServletContainerInitializer(new JettyJasperInitializer());

    scanWebApp(wac.getContextPath(), null, res);
    initWebAppContext(comp, wac);

    wac.setServer(server);

    return registerContext(wac, resourceBaseUri);
  }

  private void initWebAppContext(ComponentImpl comp, WebAppContext wac) throws IOException {
    wac.setDefaultRequestCharacterEncoding("UTF-8");
    wac.setDefaultResponseCharacterEncoding("UTF-8");

    MimeTypes.Mutable mt = wac.getMimeTypes();
    mt.addMimeMapping("html", MimeTypes.Type.TEXT_HTML_UTF_8.asString());
    mt.addMimeMapping("js", "text/javascript;charset=utf-8");
    mt.addMimeMapping("json", MimeTypes.Type.TEXT_JSON_UTF_8.asString());
    mt.addMimeMapping("txt", MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
    mt.addMimeMapping("xml", MimeTypes.Type.TEXT_XML_UTF_8.asString());

    wac.setWelcomeFiles(new String[] {"index.html", "index.html.jsp", "index.md"});
    wac.setErrorHandler(errorHandler);

    wac.setAttribute(AppHTTPServer.class.getName(), this);

    wac.setAttribute("jsonPath", (contextPath + "/" + jsonPath).replaceAll("//+", "/"));

    ServletContext sc = wac.getServletContext();
    sc.setAttribute(ServerApp.class.getName(), app);

    ServletHandler sh = wac.getServletHandler();

    mapServlets(comp, sc, sh);
    mapFilters(comp, sc, sh);
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

      sh.addServletWithMapping(holder, mapPath);
    }
  }

  private void mapFilters(ComponentImpl comp, ServletContext sc, ServletHandler sh) {
    Map<String, List<FilterMapping>> mappings = new HashMap<>();
    for (Filters f : comp.getAnnotatedMappingsFromAllReachableComponents(Filters.class)) {
      for (FilterMapping mapping : f.value()) {
        String mapPath = mapping.map();
        mappings.computeIfAbsent(mapPath, (k) -> {
          return new ArrayList<>();
        }).add(mapping);
      }
    }

    for (Map.Entry<String, List<FilterMapping>> en : mappings.entrySet()) {
      String mapPath = en.getKey();
      List<FilterMapping> list = en.getValue();

      for (FilterMapping m : list) {
        Class<? extends Filter> mapToClass = m.to();
        EnumSet<DispatcherType> types = EnumSet.copyOf(Arrays.asList(m.dispatcherTypes()));

        FilterHolder holder;
        if (mapToClass.getPackage() == AppHTTPServer.class.getPackage()) {
          Filter filter;
          try {
            filter = Objects.requireNonNull(mapToClass.getDeclaredConstructor().newInstance());
          } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
              | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(e);
          }
          holder = new FilterHolder(filter);
        } else {
          holder = new FilterHolder(mapToClass);
        }

        sh.addFilterWithMapping(holder, mapPath, types);
      }
    }
  }

  public static ServerApp getServerApp(ServletContext sc) {
    return (ServerApp) sc.getAttribute(ServerApp.class.getName());
  }

  WebAppContext registerContext(WebAppContext wac, URI webappBaseURI) {
    contexts.put(wac, new ContextMetadata(webappBaseURI));
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
            LOG.info("Regenerating " + path);
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
          LOG.info("Regeneration completed after " + (System.currentTimeMillis() - time) + "ms");
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

  public static boolean checkResourceExists(ServletContext context, String pathInContext) {
    try {
      return context.getResource(pathInContext) != null;
    } catch (MalformedURLException e) {
      return false;
    }
  }

  public boolean checkResourceExists(String path) {
    for (WebAppContext wac : contexts.keySet()) {
      String cp = wac.getContextPath();
      if (!path.startsWith(cp)) {
        continue;
      }

      String pathInContext = path.substring(cp.length());

      try {
        if (wac.getResource(pathInContext) != null) {
          return true;
        }
      } catch (MalformedURLException e) {
        // ignore
      }
    }
    return false;
  }

  static final class ContextMetadata {
    private URI webappURI;

    ContextMetadata(URI webappURI) {
      this.webappURI = webappURI;
    }

    URI getWebappURI() {
      return webappURI;
    }
  }
}
