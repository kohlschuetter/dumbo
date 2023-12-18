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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.resource.CombinedResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.json.JSONArray;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.jetty.AFSocketClientConnector;
import org.newsclub.net.unix.jetty.AFSocketServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;
import com.kohlschutter.dumbo.annotations.FilterMapping;
import com.kohlschutter.dumbo.annotations.Filters;
import com.kohlschutter.dumbo.annotations.ServletMapping;
import com.kohlschutter.dumbo.annotations.Servlets;
import com.kohlschutter.dumbo.api.DumboServer;
import com.kohlschutter.dumbo.api.DumboServiceProvider;
import com.kohlschutter.dumbo.util.DevTools;
import com.kohlschutter.dumbo.util.NativeImageUtil;
import com.kohlschutter.dumborb.security.ClassResolver;

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
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CyclomaticComplexity"})
public class DumboServerImpl implements DumboServer, DumboServiceProvider {
  private static final boolean TERMINATE_VM = Boolean.parseBoolean(System.getProperty(
      "dumbo.terminate-vm", "false"));

  private static final Logger LOG = LoggerFactory.getLogger(DumboServerImpl.class);
  private static final String JSON_PATH = "/json";
  private static final Consumer<JsonRpcContext> DEFAULT_JSONRPC_SECRET_CONSUMER = (x) -> {
  };
  private static final URI FALLBACK_URI = URI.create("http://127.0.0.0");
  private static final AtomicInteger RUNNING_SERVERS = new AtomicInteger(0);

  private final Server server;
  private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
  private final String contextPath;
  private Thread serverThread;

  private final ContextHandlerCollection contextHandlers;
  private final ServerApp app;

  private final ErrorHandler errorHandler;

  private final Map<String, String> urlPathsToRegenerate = new HashMap<>();

  private AFUNIXSocketAddress serverUNIXSocketAddress = null;

  @SuppressWarnings("PMD.LooseCoupling")
  private final LinkedHashMap<WebAppContext, ContextMetadata> contexts = new LinkedHashMap<>();

  private final Set<String> scannedFiles = new HashSet<>();

  private final Path[] cachedPaths;

  private final Semaphore serverStarted = new Semaphore(0);
  private final Semaphore pathsRegenerated = new Semaphore(0);

  private final Map<String, Path> publicUrlPathsToStaticResource = new LinkedHashMap<>();
  private final Map<String, Path> publicUrlPathsToDynamicResource = new LinkedHashMap<>();
  private final JsonRpcServlet jsonRpc;

  private final Map<String, Consumer<JsonRpcContext>> jsonRpcSecrets = new HashMap<>();

  private final boolean prewarm;

  @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  DumboServerImpl(boolean prewarm, InetAddress bindAddr, int tcpPort, String socketPath,
      final ServerApp app, final String path, final URL webappBaseURL, RequestLog requestLog,
      Path... paths) throws IOException {
    this.prewarm = prewarm;
    final int port = tcpPort == 0 ? Integer.parseInt(System.getProperty("dumbo.port", "8081"))
        : tcpPort;

    this.app = app;
    this.cachedPaths = paths != null && paths.length > 0 ? paths : null;

    this.errorHandler = new ErrorHandler();

    this.server = new Server(new QueuedThreadPool());

    if (requestLog != null) {
      server.setRequestLog(requestLog);
    }

    if (cachedPaths == null) {
      LOG.info("Starting in dynamic mode, using contents from the resource classpath");
    } else {
      if (LOG.isInfoEnabled()) {
        LOG.info("Starting in cached mode, using contents from " + Arrays.toString(cachedPaths));
      }
    }

    // server.setDumpAfterStart(true); // for debugging

    this.contextPath = "/" + (path.replaceFirst("^/", "").replaceFirst("/$", ""));
    app.registerCloseable(new Closeable() {
      @Override
      public void close() throws IOException {
        shutdown();
      }
    });

    contextHandlers = new ContextHandlerCollection();

    app.init(this, path);

    WebAppContext wac;
    if (webappBaseURL != null) {
      wac = initMainWebAppContext(webappBaseURL);
    } else {
      wac = initMainWebAppContextPreprocessed(paths);
    }

    this.jsonRpc = new JsonRpcServlet();
    jsonRpc.setServer(this);
    ServletHolder sh = new ServletHolder(this.jsonRpc);
    sh.setInitOrder(0); // initialize right upon start
    wac.addServlet(sh, JSON_PATH);

    wac.setServer(server);

    app.initComponents(this);

    server.setHandler(contextHandlers);
    server.setConnectors(initConnectors(bindAddr, port, socketPath, server));
  }

  private WebAppContext initMainWebAppContext(URL webappBaseURL) throws IOException {
    URI webappBaseURI;
    try {
      webappBaseURI = webappBaseURL.toURI();
    } catch (URISyntaxException e1) {
      throw new IllegalStateException(e1);
    }

    Resource res;
    try {
      res = combinedResource(app.getWebappWorkDir().toURI(), webappBaseURL.toURI(), NativeImageUtil
          .walkResources("jettydir-overlay/", DumboServerImpl.class::getResource).toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
    final WebAppContext wac = new WebAppContext(res, contextPath);
    wac.setBaseResource(res);

    initMainWebAppContextCommon(wac);

    initWebAppContext(app.getApplicationExtensionImpl(), wac);

    registerContext(wac, webappBaseURI);

    if (cachedPaths == null) {
      scanWebApp(wac.getContextPath(), res);
    }

    return wac;
  }

  private WebAppContext initMainWebAppContextPreprocessed(Path... paths) throws IOException {
    Resource res = combinedResource(app.getWebappWorkDir().toPath(), paths);
    final WebAppContext wac = new WebAppContext(res, contextPath);
    wac.setBaseResource(res);
    initMainWebAppContextCommon(wac);
    initWebAppContext(app.getApplicationExtensionImpl(), wac);
    registerContext(wac, null);
    return wac;
  }

  private void initMainWebAppContextCommon(WebAppContext wac) throws IOException {
    wac.setLogger(LOG);
    wac.addServletContainerInitializer(new JettyJasperInitializer());
    wac.setTempDirectory(new File(app.getWorkDir(), "jetty.tmp"));
    wac.setTempDirectoryPersistent(true);
  }

  private Resource combinedResource(URI... uris) throws IOException {
    ResourceFactory factory = ResourceFactory.root();
    List<Resource> list = new ArrayList<>(uris.length);
    for (URI u : uris) {
      Resource r;

      try {
        Paths.get(u);
      } catch (FileSystemNotFoundException e) {
        String uriPath = u.getSchemeSpecificPart();
        int sep = uriPath.indexOf("!/");
        if (sep == -1) {
          throw e;
        }
        FileSystems.newFileSystem(u, Collections.emptyMap());
      }

      r = factory.newResource(u);
      list.add(r);
    }
    return ResourceFactory.combine(list);
  }

  private Resource newResourceCreateIfNecessary(ResourceFactory rf, Path p) throws IOException {
    Resource r = rf.newResource(p);
    if (r == null) {
      if (!Files.exists(p)) {
        LOG.warn("Creating directory for missing path {}", p);
        Files.createDirectories(p);
        r = rf.newResource(p);
      }
    }
    return r;
  }

  private Resource combinedResource(Path first, Path... paths) throws IOException {
    ResourceFactory rf = ResourceFactory.root();
    List<Resource> resources = new ArrayList<>();

    if (first != null) {
      first = first.toRealPath(LinkOption.NOFOLLOW_LINKS);
      Resource firstR = newResourceCreateIfNecessary(rf, first);
      if (firstR == null) {
        LOG.warn("Could not create Resource for path {}", first);
      } else {
        resources.add(firstR);
      }
    }

    for (Path p : paths) {
      p = p.toRealPath(LinkOption.NOFOLLOW_LINKS); // NOPMD.AvoidReassigningLoopVariables
      Resource r = newResourceCreateIfNecessary(rf, p);
      if (r == null) {
        LOG.warn("Could not create Resource for path {}", p);
      } else {
        resources.add(r);
      }
    }

    return ResourceFactory.combine(resources);
  }

  static URL getWebappBaseURL(final ServerApp app) {
    URL u;

    u = NativeImageUtil.walkResources("webapp/", app
        .getApplicationExtensionImpl()::getComponentResource);
    if (u != null) {
      return u;
    }

    // FIXME
    String path = "/" + app.getApplicationClass().getPackage().getName().replace('.', '/')
        + "/webapp/";
    u = NativeImageUtil.walkResources(path, app.getApplicationClass()::getResource);
    Objects.requireNonNull(u, () -> {
      return "Resource path is missing: " + path;
    });

    return u;
  }

  private Path checkValidPath(Resource resource, List<Path> validPaths) {
    Path match;
    if (resource instanceof CombinedResource) {
      CombinedResource cr = (CombinedResource) resource;
      for (Resource r : cr) {
        if ((match = checkValidPath(r, validPaths)) != null) {
          return match;
        }
      }
      return null;
    } else {
      return checkValidPath(resource.getPath(), validPaths);
    }
  }

  private Path checkValidPath(Path resourcePath, List<Path> validPaths) {
    for (Path p : validPaths) {
      try {
        if (resourcePath.startsWith(p)) {
          return p;
        }
      } catch (ProviderMismatchException e) {
        System.err.println("ERROR: " + e + " checking " + resourcePath + " startsWith " + p);
        e.printStackTrace();
        return null;
      }
    }
    return null;
  }

  /**
   * Scan the webapp's resources for files that we should request via HTTP (to trigger caching,
   * etc.).
   *
   * @param contextPrefix The context prefix.
   * @param dir The directory resource.
   * @throws IOException on error.
   */
  @SuppressWarnings("PMD.CognitiveComplexity")
  private void scanWebApp(String context, Resource dir) throws IOException {
    LOG.info("Scanning contents of context {} from {}", context, dir);
    scanWebApp(context, dir, null);
  }

  @SuppressWarnings("PMD.CognitiveComplexity")
  private void scanWebApp(String context, Resource dir, List<Path> dirPrefixes) throws IOException {
    String key = dir.toString();
    if (!scannedFiles.add(key)) {
      // already scanned
      return;
    }

    if (dirPrefixes == null) {
      if (dir instanceof CombinedResource) {
        CombinedResource cr = (CombinedResource) dir;
        dirPrefixes = new ArrayList<>();
        for (Resource r : cr.getResources()) {
          dirPrefixes.add(r.getPath());
        }
      } else {
        dirPrefixes = Collections.singletonList(dir.getPath());
      }
    } else {
      if (checkValidPath(dir, dirPrefixes) == null) {
        LOG.warn("Invalid directory path: {}", dir);
        return;
      }
    }

    for (Resource r : dir.list()) {
      if (r.isDirectory()) {
        scanWebApp(context, r, dirPrefixes);
        continue;
      }

      Path path = r.getPath();

      Path okPrefix = checkValidPath(r, dirPrefixes);
      if (okPrefix == null) {
        LOG.warn("Invalid directory path: {}", dir);
        continue;
      }

      Path relativePath = okPrefix.relativize(path);

      String name = path.getName(path.getNameCount() - 1).toString();

      String contextPrefix;
      if (context.endsWith("/")) {
        contextPrefix = context;
      } else {
        contextPrefix = context + "/";
      }

      String urlPath = contextPrefix + relativePath.toString();

      String cachedFile = processFileName(name);
      if (cachedFile == null) {
        // don't include
        continue;
      }

      final String publicUrlPath;
      final Path resourcePath;
      if (cachedFile.equals(name)) {
        publicUrlPath = urlPath;
        resourcePath = path;
      } else {
        Path cachedRelativePath = relativePath.resolveSibling(cachedFile);
        publicUrlPath = contextPrefix + cachedRelativePath;
        resourcePath = dirPrefixes.get(0).resolve(cachedRelativePath.toString());
        cachedFile = contextPrefix + "/" + cachedFile;
        urlPathsToRegenerate.put(urlPath, publicUrlPath);
      }

      if (isStaticFileName(cachedFile)) {
        publicUrlPathsToStaticResource.computeIfAbsent(publicUrlPath, (p) -> resourcePath);
      } else {
        publicUrlPathsToDynamicResource.computeIfAbsent(publicUrlPath, (p) -> resourcePath);
      }
    }
  }

  private static boolean isStaticFileName(String name) {
    return !name.endsWith(".jsp") || name.endsWith(".html.jsp");
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
   * @throws IOException on error.
   */
  public WebAppContext registerContext(ComponentImpl comp, final String contextPrefix,
      final URL pathToWebAppURL) throws IOException {
    URI resourceBaseUri;
    try {
      resourceBaseUri = pathToWebAppURL.toURI();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }

    String prefix = (contextPath + contextPrefix).replaceAll("//+", "/");
    String relativePrefix = prefix.replaceAll("^/+", "");

    final File contextWorkDir = new File(app.getWebappWorkDir(), relativePrefix);
    Files.createDirectories(contextWorkDir.toPath());

    Resource res;
    if (cachedPaths != null) {
      Path[] paths = new Path[cachedPaths.length];

      for (int i = 0, n = paths.length; i < n; i++) {
        paths[i] = cachedPaths[i].resolve(relativePrefix);
      }

      res = combinedResource(contextWorkDir.toPath(), paths);
    } else {

      try {
        res = ResourceFactory.root().newResource(List.of(contextWorkDir.toURI(), resourceBaseUri,
            NativeImageUtil.walkResources("jettydir-overlay/", DumboServerImpl.class::getResource)
                .toURI()));
      } catch (URISyntaxException e) {
        throw new IllegalStateException(e);
      }
    }

    WebAppContext wac = new WebAppContext(res, prefix);

    wac.setBaseResource(res);
    wac.setLogger(LOG);
    wac.addServletContainerInitializer(new JettyJasperInitializer());

    if (cachedPaths == null) {
      scanWebApp(wac.getContextPath(), res);
    }
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

    wac.setAttribute(DumboServerImpl.class.getName(), this);

    wac.setAttribute("jsonPath", (contextPath + "/" + JSON_PATH).replaceAll("//+", "/"));

    ServletContext sc = wac.getServletContext();
    sc.setAttribute(ServerApp.class.getName(), app);

    ServletHandler sh = wac.getServletHandler();

    mapServlets(comp, sh);
    mapFilters(comp, sh);
  }

  private void mapServlets(ComponentImpl comp, ServletHandler sh) {
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
    holderDefaultServlet.setInitParameter("stylesheet", "/css/jetty-dir.css");

    for (Map.Entry<String, ServletMapping> en : mappings.entrySet()) {
      String mapPath = en.getKey();
      ServletMapping m = en.getValue();

      Class<? extends Servlet> mapToClass = m.to();

      ServletHolder holder;

      if (mapToClass.getPackage().equals(DumboServerImpl.class.getPackage())) {
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

  private void mapFilters(ComponentImpl comp, ServletHandler sh) {
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
        if (mapToClass.getPackage().equals(DumboServerImpl.class.getPackage())) {
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
    if (webappBaseURI != null) {
      contexts.put(wac, new ContextMetadata(webappBaseURI));
    }
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

  @SuppressWarnings("PMD.CognitiveComplexity")
  private CompletableFuture<Void> regeneratePaths() throws Exception {
    if (urlPathsToRegenerate.isEmpty()) {
      return CompletableFuture.completedFuture((Void) null);
    }

    return CompletableFuture.runAsync(() -> {
      try {
        URI serverURI = getURI();

        JSONArray jsonMethods = jsonRpc.getBridge().getSystemMethods();
        for (WebAppContext wac : contexts.keySet()) {
          wac.setAttribute("dumborb.json.methods", jsonMethods.toString());
        }

        String serverURIBase = new URI(serverURI.getScheme(), serverURI.getUserInfo(), serverURI
            .getHost(), serverURI.getPort(), null, null, null).toString();
        // ClientConnector clientConnector = new ClientConnector();
        HttpClient client = newServerHttpClient();
        client.start();

        long time = System.currentTimeMillis();
        CountDownLatch cdl = new CountDownLatch(urlPathsToRegenerate.size());
        client.setMaxConnectionsPerDestination(1);
        try {
          if (LOG.isInfoEnabled()) {
            LOG.info("Regenerating " + cdl.getCount() + " paths...");
          }
          for (String path : urlPathsToRegenerate.keySet()) {
            LOG.info("Regenerating {}", path);
            String uri = serverURIBase + path + "?reload=true";
            try {
              client.newRequest(uri).method(HttpMethod.HEAD).send(new CompleteListener() {

                @Override
                public void onComplete(Result result) {
                  if (result.isFailed()) {
                    if (LOG.isWarnEnabled()) {
                      LOG.warn("Regeneration failed for path: " + path, result.getFailure());
                    }
                  } else if (result.getResponse().getStatus() != HttpServletResponse.SC_OK) {
                    if (LOG.isWarnEnabled()) {
                      LOG.warn("Regeneration failed with response " + result.getResponse()
                          + " for path: " + path);
                    }
                  }
                  cdl.countDown();
                }
              });
            } catch (Throwable t) { // NOPMD
              t.printStackTrace();
            }
          }
          if (!cdl.await(1, TimeUnit.MINUTES)) {
            if (LOG.isWarnEnabled()) {
              LOG.warn("Regeneration is taking a long time");
            }
          }
          cdl.await();
          if (LOG.isInfoEnabled()) {
            LOG.info("Regeneration completed after " + (System.currentTimeMillis() - time) + "ms");
          }
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

  private static void copyResourcesToMappedDir(Map<String, Path> resources, Path outputBaseDir,
      boolean sourceMaps) throws IOException {

    // delete all existing files
    Files.walkFileTree(outputBaseDir, new FileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (!dir.equals(outputBaseDir)) {
          Files.delete(dir);
        }
        return FileVisitResult.CONTINUE;
      }
    });

    for (Map.Entry<String, Path> en : resources.entrySet()) {
      String urlPath = en.getKey().replaceAll("^/+", "");
      if (!sourceMaps) {
        if ("sourcemaps".equals(urlPath) || urlPath.startsWith("sourcemaps/") || urlPath.endsWith(
            ".js.map")) {
          continue;
        }
      }

      Path serverPath = en.getValue();

      Path outputPath = outputBaseDir.resolve(urlPath);

      Path parentPath = outputPath.getParent();
      if (parentPath != null) {
        Files.createDirectories(parentPath);
      }
      Files.copy(serverPath, outputPath, StandardCopyOption.REPLACE_EXISTING);

      if (LOG.isDebugEnabled()) {
        LOG.debug("File for path {} is stored at {}", en.getKey(), en.getValue());
      }
    }
  }

  /**
   * Starts the HTTP Server to regenerate resources, copies all resources, then stops the server.
   *
   * @param staticOut The target path for static content.
   * @param dynamicOut The target path for dynamic content (jsp files, etc.)
   * @param sourceMaps Whether {@code /sourcemaps} should be included.
   * @throws IOException on error.
   * @throws InterruptedException on interruption.
   */
  public void generateFiles(Path staticOut, Path dynamicOut, boolean sourceMaps) throws IOException,
      InterruptedException {
    boolean started = server.isStarted();
    if (!started) {
      start();
    }
    LOG.info("Generating cached version at (static:) {} and (dynamic:) {}", staticOut, dynamicOut);
    pathsRegenerated.acquire();
    try {
      copyResourcesToMappedDir(publicUrlPathsToStaticResource, staticOut, sourceMaps);
      copyResourcesToMappedDir(publicUrlPathsToDynamicResource, dynamicOut, sourceMaps);
      LOG.info("Generated cached version at (static:) {} and (dynamic:) {}", staticOut, dynamicOut);
      if (!started) {
        shutdown();
      }
    } finally {
      pathsRegenerated.release();
    }
  }

  private synchronized void start0() {
    Thread t = serverThread;
    if (t != null) {
      return;
    }

    // forcibly reset counts
    serverStarted.tryAcquire(serverStarted.availablePermits());
    pathsRegenerated.tryAcquire(pathsRegenerated.availablePermits());

    serverThread = t = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          RUNNING_SERVERS.incrementAndGet();
          try {
            server.start();

            regeneratePaths().thenRun(() -> prewarmContent()).thenAccept((v) -> pathsRegenerated
                .release());

            DevTools.init();

            serverStarted.release();
            CompletableFuture.runAsync(DumboServerImpl.this::onServerStart);
            try {
              server.join();
            } catch (InterruptedException e) {
              if (shutdownRequested.get()) {
                // all good
              } else {
                throw e;
              }
            }
          } finally {
            LOG.info("Shutting down ...");
            RUNNING_SERVERS.decrementAndGet();
            onServerStop();
          }
        } catch (Exception e) {
          onServerException(e);
        }
      }
    });
    t.setName("ServerThread");
    t.start();
  }

  private void prewarmContent() {
    if (!prewarm) {
      LOG.info("Prewarm disabled");
      return;
    }
    if (cachedPaths == null) {
      LOG.info("Prewarm enabled, but no cached paths");
      return;
    }
    try {
      for (Path p : cachedPaths) {
        LOG.info("Prewarming content from {}", p);
        visitStaticContent(p);
      }
    } catch (Exception e) {
      LOG.warn("Error while visiting static context", e);
    }
  }

  private void visitStaticContent(Path p) throws Exception {
    HttpClient client = newServerHttpClient();
    client.start();

    URI baseURI = getURI();

    List<Path> list = Files.walk(p).filter(Files::isRegularFile).collect(Collectors.toList());
    CountDownLatch cdl = new CountDownLatch(list.size());

    list.forEach((f) -> {
      String relativeUrl = p.relativize(f).toString();
      LOG.debug("Request prewarm: {}", relativeUrl);
      client.newRequest(baseURI.resolve(relativeUrl)).method(HttpMethod.GET).send(
          new CompleteListener() {

            @Override
            public void onComplete(Result result) {
              if (result.isSucceeded()) {
                LOG.debug("Prewarm OK: {}", relativeUrl);
              } else {
                if (LOG.isWarnEnabled()) {
                  LOG.warn("Prewarm failed with status {} for {} ", result.getResponse()
                      .getStatus(), relativeUrl);
                }
              }
              cdl.countDown();
            }
          });
    });

    cdl.await();
  }

  @Override
  public DumboServer awaitIdle() throws InterruptedException {
    pathsRegenerated.acquire();
    pathsRegenerated.release();
    return this;
  }

  @Override
  public synchronized DumboServerImpl start() throws InterruptedException {
    if (server.isStarted()) {
      return this;
    }
    startDontWait();
    serverStarted.acquire();
    return this;
  }

  /**
   * Starts the HTTP Server, runs it in a separate thread. The call will only return when the server
   * has been shut down.
   */
  public void startAndWait() {
    try {
      start();
      serverThread.join();
    } catch (InterruptedException e) {
      onServerException(e);
    }
  }

  /**
   * Starts the HTTP Server, returning as soon as possible, continuing the start a separate thread.
   */
  public synchronized void startDontWait() {
    start0();
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
    if (!TERMINATE_VM) {
      return;
    }
    if (RUNNING_SERVERS.get() <= 0) {
      // Make sure the JVM exits -- Maven's exec:java may spawn extra threads...
      new Thread() {
        {
          setDaemon(true);
        }

        @Override
        @SuppressFBWarnings("DM_EXIT")
        public void run() {
          try {
            Thread.sleep(5000);
          } catch (InterruptedException ignore) {
            // ignored
          }
          if (RUNNING_SERVERS.get() <= 0) {
            System.exit(0); // NOPMD.DoNotTerminateVM
          }
        }
      }.start();
    }
  }

  /**
   * This method is called upon any exception during server startup or shutdown.
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
    shutdownRequested.set(true);
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
   * @param address The bind address
   * @param tcpPort The bind port.
   * @param socketPath
   * @param targetServer The target server.
   * @return The connector(s).
   * @throws IOException on error.
   */
  protected Connector[] initConnectors(InetAddress address, int tcpPort, String socketPath,
      Server targetServer) throws IOException {
    String dumboSocketId;
    ServerConnector tcpConn;
    if (tcpPort == -1) {
      dumboSocketId = UUID.randomUUID().toString();
      tcpConn = null;
    } else {
      tcpConn = initDefaultTCPConnector(address, tcpPort, targetServer);
      dumboSocketId = String.valueOf(tcpConn.getPort());
    }

    Connector unixConnector;
    if (socketPath == null || socketPath.isEmpty()) {
      serverUNIXSocketAddress = null;
      unixConnector = null;
    } else {
      if ("auto".equals(socketPath) || "*".equals(socketPath)) {
        socketPath = "/tmp/dumbo-" + dumboSocketId + ".sock";
      }
      serverUNIXSocketAddress = AFUNIXSocketAddress.of(new File(socketPath));
      unixConnector = initUnixConnector(targetServer, serverUNIXSocketAddress);
    }

    if (tcpConn == null) {
      return new Connector[] {unixConnector};
    } else if (unixConnector == null) {
      return new Connector[] {tcpConn};
    } else {
      return new Connector[] {tcpConn, unixConnector};
    }
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
  protected ServerConnector initDefaultTCPConnector(InetAddress addr, int port, Server targetServer)
      throws IOException {
    ServerConnector connector = new ServerConnector(targetServer, newHttpConnectionFactory());

    connector.setPort(port <= 0 ? 0 : port);
    connector.setReuseAddress(true);
    connector.setReusePort(true);
    connector.setHost(addr == null ? null : addr.getHostAddress());

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

  @Override
  public URI getURI() {
    URI uri = server.getURI();
    if (uri == null) {
      return FALLBACK_URI;
    } else {
      return uri;
    }
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

  @SuppressWarnings("PMD.CognitiveComplexity")
  public boolean checkResourceExists(String path) {
    if (cachedPaths != null) {
      String relativePath = path.replaceFirst("^/+", "");
      for (Path p : cachedPaths) {
        if (Files.exists(p.resolve(relativePath))) {
          return true;
        }
      }
      return false;
    }

    for (WebAppContext wac : contexts.keySet()) {
      String cp = wac.getContextPath();
      if (!path.startsWith(cp)) {
        continue;
      }

      String pathInContext = path.substring(cp.length());
      if (pathInContext.isEmpty()) {
        return false;
      } else if (pathInContext.charAt(0) != '/') {
        pathInContext = "/" + pathInContext;
      }

      try {
        if (wac.getResource(pathInContext) != null) {
          return true;
        } else if (wac.getResource(pathInContext + ".jsp") != null) {
          return true;
        } else {
          // if resource is "something.js", also check "something.jsp.js", for example.
          // this code will only run for filename suffixes less than 10 characters
          int lastDot = pathInContext.lastIndexOf('.');
          if (lastDot != -1 && lastDot >= pathInContext.length() - 10 && pathInContext.indexOf('/',
              lastDot + 1) == -1) {
            String path2 = pathInContext.substring(0, lastDot) + ".jsp" + pathInContext.substring(
                lastDot);
            if (wac.getResource(path2) != null) {
              return true;
            }
          }
        }
      } catch (MalformedURLException e) {
        // ignore
      }
    }
    return false;
  }

  static final class ContextMetadata {
    private final URI webappURI;

    ContextMetadata(URI webappURI) {
      this.webappURI = webappURI;
    }

    URI getWebappURI() {
      return webappURI;
    }
  }

  boolean isCachedMode() {
    return cachedPaths != null;
  }

  @Override
  public <T> T getDumboService(Class<T> clazz) {
    return jsonRpc.getRPCService(clazz);
  }

  /**
   * Returns a new JSON-RPC client that is connected to this server's json-rpc service.
   *
   * @return The new client.
   */
  public JsonRpcClient newJsonRpcClient() {
    if (!server.isStarted()) {
      throw new IllegalStateException("Server is not (yet) started");
    }
    URL jsonRpcUrl;
    try {
      jsonRpcUrl = getURI().resolve(JSON_PATH).toURL();
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }

    ClassResolver classResolver = ClassResolver.withDefaults();
    return new JsonRpcClient(jsonRpcUrl, classResolver);
  }

  Consumer<JsonRpcContext> getJsonRpcTestSecretConsumer(String secret) {
    return jsonRpcSecrets.getOrDefault(secret, DEFAULT_JSONRPC_SECRET_CONSUMER);
  }

  public void setJsonRpcTestSecretConsumer(String secret,
      Consumer<JsonRpcContext> contextConsumer) {
    jsonRpcSecrets.put(secret, contextConsumer);
  }

  static DumboServerImpl getInstance(ServletContext context) {
    return (DumboServerImpl) context.getAttribute(DumboServerImpl.class.getName());
  }
}
