/*
 * Copyright 2022-2025 Christian Kohlschütter
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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;
import com.kohlschutter.dumbo.annotations.EventHandlers;
import com.kohlschutter.dumbo.annotations.Services;
import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboComponent;
import com.kohlschutter.dumbo.api.DumboServiceProvider;
import com.kohlschutter.dumbo.api.DumboSession;
import com.kohlschutter.dumbo.api.EventHandler;
import com.kohlschutter.dumbo.console.ConsoleService;
import com.kohlschutter.dumbo.exceptions.ExtensionDependencyException;

/**
 * Internal base class for a lightweight Server-based application.
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
public final class ServerApp implements Closeable, DumboServiceProvider {
  private static final Logger LOG = LoggerFactory.getLogger(ServerApp.class);

  @SuppressWarnings("PMD.LooseCoupling")
  private final LinkedHashMap<Class<?>, ExtensionImpl> extensions = new LinkedHashMap<>();
  private final Map<Class<?>, Object> instances = new HashMap<>();
  @SuppressWarnings("PMD.LooseCoupling")
  private final LinkedHashSet<EventHandler> eventHandlers = new LinkedHashSet<>();
  private final List<Closeable> closeables = Collections.synchronizedList(
      new ArrayList<Closeable>());

  private final AtomicBoolean initDone = new AtomicBoolean(false);
  private volatile boolean closed = false;
  private RPCRegistry rpcRegistry;

  private boolean staticDesignMode = false;

  private final Class<? extends DumboApplication> applicationClass;
  private final ExtensionImpl applicationExtensionImpl;

  private final Map<Class<? extends DumboComponent>, Set<Class<? extends DumboComponent>>> componentToSubComponentMap =
      new HashMap<>();

  private final Map<ImplementationIdentity<?>, Object> implementationIdentities =
      new WeakHashMap<>();

  private File workDir;
  private File webappWorkDir;
  private File jspWorkDir;

  private DumboServerImpl appServer = null;

  private final URL webappBaseURL;

  private final String prefix;

  private final String contextPath;

  private final JsonRpcServlet jsonRpc = new JsonRpcServlet();

  public ServerApp(String prefix, Class<? extends DumboApplication> applicationClass,
      Supplier<URL> webappBaseURLsupplier) {
    prefix = sanitzePrefix(prefix);
    this.prefix = prefix;
    this.contextPath = ("/" + (prefix.replaceFirst("^/", "").replaceFirst("/$", ""))).replaceAll(
        "//+", "/");

    this.applicationClass = applicationClass;

    this.applicationExtensionImpl = new ExtensionImpl(applicationClass, true);

    resolveExtensions();
    initEventHandlers();

    this.webappBaseURL = webappBaseURLsupplier == null ? DumboServerImpl.getWebappBaseURL(this)
        : webappBaseURLsupplier.get();
  }

  private static String sanitzePrefix(String prefix) {
    if (prefix == null) {
      return "";
    } else {
      if (prefix.startsWith("/")) {
        prefix = prefix.substring(1);
      }
      if (prefix.endsWith("/")) {
        prefix = prefix.substring(0, prefix.length() - 1);
      }
      return prefix;
    }
  }

  /**
   * Returns an implementation for the given component identity.
   *
   * This data structure can be used by servlets etc. to store information at the application level
   * in a secure way: only the code that has access to the given {@link ImplementationIdentity} can
   * access the implementation.
   *
   * @param identity The implementation identity.
   * @return The implementation instance.
   * @throws IOException on error.
   */
  @SuppressWarnings({"unchecked"})
  public <K> @NonNull K getImplementationByIdentity(ImplementationIdentity<K> identity,
      ImplementationIdentity.Supplier<@NonNull K> supplier) throws IOException {
    K instance = (K) implementationIdentities.get(identity);
    if (instance == null) {
      instance = Objects.requireNonNull(supplier.get());
      implementationIdentities.put(identity, instance);
    }
    return instance;
  }

  private void resolveExtensions() throws ExtensionDependencyException {
    @SuppressWarnings("PMD.LooseCoupling")
    LinkedHashSet<Class<?>> reachableComponents = applicationExtensionImpl.getReachableComponents();

    for (Class<?> compClass : reachableComponents) {
      if (extensions.containsKey(compClass)) {
        continue;
      }

      @SuppressWarnings("unchecked")
      ExtensionImpl ext = compClass.equals(applicationClass) ? applicationExtensionImpl
          : new ExtensionImpl((Class<? extends DumboComponent>) compClass, false);

      extensions.put(compClass, ext);
    }

    for (ExtensionImpl ext : extensions.values()) {
      ext.verifyDependencies(this, extensions.keySet());
    }
  }

  private void initEventHandlers() {
    List<Class<? extends EventHandler>> eventHandlerClasses = applicationExtensionImpl
        .getAnnotations(EventHandlers.class).stream().map((s) -> s.value()).flatMap(Stream::of)
        .distinct().collect(Collectors.toList());

    if (EventHandler.class.isAssignableFrom(applicationClass)) {
      eventHandlers.add((EventHandler) getInstance(applicationClass));
    }

    for (Class<? extends EventHandler> c : eventHandlerClasses) {
      eventHandlers.add(getInstance(c));
    }
  }

  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  synchronized void init(DumboServerImpl server, String path) throws IOException {
    if (appServer != null) {
      throw new IllegalStateException("Already initialized");
    }
    appServer = server;

    // also see AppHTTPServer
    File dir = Files.createTempDirectory("dumbo-workdir").toRealPath().toFile();

    this.workDir = new File(dir, path);
    Files.createDirectories(workDir.toPath());

    // FIXME move to AppHTTPServer
    Runtime.getRuntime().addShutdownHook(new Thread() {
      private void delete(File d) {
        File[] files = d.listFiles();
        if (files != null) {
          for (File f : files) {
            if (f.isDirectory()) {
              delete(f);
            }
            deleteFile(f);
          }
        }
        deleteFile(d);
      }

      @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
      private void deleteFile(File f) {
        f.delete();
      }

      @Override
      public void run() {
        delete(dir);
      }
    });
    this.webappWorkDir = new File(this.workDir, "webapp");
    this.jspWorkDir = new File(this.workDir, "jsp");
    Files.createDirectories(webappWorkDir.toPath());

    LOG.info("Workdir: {}", workDir);
  }

  void initComponents(DumboServerImpl server) throws IOException {
    if (!initDone.compareAndSet(false, true)) {
      throw new IllegalStateException("App is already initialized");
    }
    for (ExtensionImpl ext : extensions.values()) {
      ext.initComponent(this, server);

      ext.getComponentToSubComponentsMap().forEach((k, v) -> {
        getComponentToSubComponentMap().computeIfAbsent(k, (e) -> {
          return new HashSet<Class<? extends DumboComponent>>();
        }).addAll(v);
      });
    }
  }

  /**
   * Registers the designated RPC services.
   *
   * @param registry The target RPC registry.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  void initRPC(RPCRegistry registry) {
    this.rpcRegistry = registry;

    registry.registerRPCService(ConsoleService.class, new ConsoleService() {

      @Override
      public Object requestNextChunk() {
        DumboSession session = DumboSessionImpl.getSession();

        return ((ConsoleImpl) session.getConsole()).getConsoleService().requestNextChunk();
      }
    });

    List<Class<?>> serviceClasses = applicationExtensionImpl.getAnnotations(Services.class).stream()
        .map((s) -> s.value()).flatMap(Stream::of).distinct().collect(Collectors.toList());
    for (Class<?> serviceClass : serviceClasses) {
      Class<?>[] interfaces = serviceClass.getInterfaces();
      if (interfaces.length == 0) {
        throw new IllegalStateException("Class does not implement any interfaces: " + serviceClass);
      }
      registry.registerRPCService((Class) interfaces[0], getInstance(serviceClass));
    }
  }

  /**
   * Registers a {@link Closeable} instance that should be closed automatically when the application
   * shuts down.
   *
   * @param cl The {@link Closeable} to add.
   * @return The {@link Closeable} itself.
   */
  public <T extends Closeable> T registerCloseable(final T cl) {
    closeables.add(cl);
    return cl;
  }

  /**
   * Called when an instance of this app has loaded.
   *
   * @param session The session for this instance.
   */
  void onAppLoaded(final DumboSession session) {
    eventHandlers.forEach((eh) -> eh.onAppLoaded(session));
  }

  /**
   * This method will be called upon application start.
   */
  private void onAppStart() {
    // FIXME call event handlers
  }

  @Override
  public void close() throws IOException {
    if (closed) {
      return;
    }
    for (ListIterator<Closeable> lit = closeables.listIterator(closeables.size()); lit
        .hasPrevious();) {
      @SuppressWarnings("resource")
      Closeable cl = lit.previous();
      try {
        cl.close();
      } catch (Exception e) {
        if (LOG.isInfoEnabled()) {
          LOG.info("Error while closing object of type " + cl.getClass(), e);
        }
      }
    }
    closed = true;
  }

  /**
   * Checks whether this app has been closed already.
   *
   * @return {@code true} if closed.
   */
  public boolean isClosed() {
    return closed;
  }

  /**
   * Returns the extensions registered with this app.
   *
   * @return The collection of extensions.
   */
  Collection<ExtensionImpl> getExtensions() {
    return extensions.values();
  }

  /**
   * Checks whether static design mode is enabled.
   *
   * If enabled, {@link #onStart()} will not be called.
   *
   * @return {@code true} if static design mode is enabled.
   */
  boolean isStaticDesignMode() {
    return staticDesignMode;
  }

  @SuppressFBWarnings("AT_STALE_THREAD_WRITE_OF_PRIMITIVE")
  void setStaticDesignMode(boolean on) {
    this.staticDesignMode = on;
  }

  RPCRegistry getRPCRegistry() {
    return rpcRegistry;
  }

  /**
   * Returns how many concurrent pageIds should be maintained per HTTP session; use {@code -1} for
   * "unlimited", {@code 0} for "none" (which is probably not what you want).
   *
   * By default, a sensible limit is returned.
   *
   * @return The maximum, {@code -1} for unlimited, {@code 0} for none
   */
  public int getMaximumPagesPerSession() {
    return 16;
  }

  public Class<? extends DumboApplication> getApplicationClass() {
    return applicationClass;
  }

  ComponentImpl getApplicationExtensionImpl() {
    return applicationExtensionImpl;
  }

  /**
   * Look up a resource in the resource path of the app and any registered {@link ExtensionImpl}.
   *
   * @param path The path to look up (usually relative).
   * @return The {@link URL} pointing to the resource, or {@code null} if not found/not accessible.
   */
  public URL getResource(String path) {
    URL resource = applicationExtensionImpl.getComponentClass().getResource(path);
    if (resource != null) {
      return resource;
    }
    for (ComponentImpl ext : getExtensions()) {
      resource = ext.getComponentResource(path);
      if (resource != null) {
        return resource;
      }
    }
    return null;
  }

  /**
   * Called after initialization, unless static design mode is enabled.
   *
   * This is the place where you want to start the "main" operations.
   *
   * @see #initRPC(RPCRegistry)
   */
  void onStart() {
    new Thread() {
      @Override
      public void run() {
        try {
          onAppStart();
        } catch (Exception e) {
          LOG.error("Error upon app start", e);
        }
      }
    }.start();
  }

  /**
   * Gets the app's instance (or creates a new instance of) the given class, trying several
   * construction methods, starting with a constructor that takes no parameters.
   *
   * @param <T> The instance type.
   * @param clazz The class to instantiate.
   * @return The instance.
   * @throws IllegalStateException if the class could not be initialized.
   */
  synchronized <T> T getInstance(Class<T> clazz) throws IllegalStateException {
    @SuppressWarnings("unchecked")
    T instance = (T) instances.get(clazz);
    if (instance != null) {
      return instance;
    }

    try {
      try {
        instance = clazz.getDeclaredConstructor().newInstance();
        instances.put(clazz, instance);
        return instance;
      } catch (NoSuchMethodException e) {
        // ignore
      }
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | SecurityException e) {
      throw new IllegalStateException("Could not initialize " + clazz, e);
    }

    throw new IllegalStateException("Could not find a way to initialize " + clazz);
  }

  public File getWorkDir() {
    return workDir;
  }

  public File getWebappWorkDir() {
    return webappWorkDir;
  }

  public File getJspWorkDir() {
    return jspWorkDir;
  }

  Map<Class<? extends DumboComponent>, Set<Class<? extends DumboComponent>>> getComponentToSubComponentMap() {
    return componentToSubComponentMap;
  }

  public URL getWebappBaseURL() {
    return webappBaseURL;
  }

  public String getPrefix() {
    return prefix;
  }

  public String getContextPath() {
    return contextPath;
  }

  @Override
  public <T> T getDumboService(Class<T> clazz) {
    return jsonRpc.getRPCService(clazz);
  }

  public void setServer(DumboServerImpl dumboServerImpl) {
    jsonRpc.setServer(dumboServerImpl);
  }

  JsonRpcServlet getJsonRpc() {
    return jsonRpc;
  }
}
