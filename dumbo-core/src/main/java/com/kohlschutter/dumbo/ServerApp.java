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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.console.ConsoleService;
import com.kohlschutter.dumbo.util.IteratorIterable;

/**
 * Internal base class for a lightweight Server-based application.
 */
public abstract class ServerApp extends Component implements Closeable, Cloneable {
  private static final Logger LOG = LoggerFactory.getLogger(ServerApp.class);
  private final LinkedHashMap<Class<?>, Extension> extensions = new LinkedHashMap<>();

  private AtomicBoolean initDone = new AtomicBoolean(false);
  private volatile boolean closed = false;
  private RPCRegistry rpcRegistry;

  private boolean staticDesignMode = false;

  protected ServerApp() {
    super();
    resolveExtensions();
  }

  private void resolveExtensions() throws ExtensionDependencyException {
    LinkedHashMap<Class<?>, AtomicInteger> extensionClasses = new LinkedHashMap<>();
    registerExtensions(extensionClasses, getClass());

    ArrayList<Entry<Class<?>, AtomicInteger>> extensionsRanked = new ArrayList<>(extensionClasses
        .entrySet());
    extensionsRanked.sort((a, b) -> (b.getValue().get() - a.getValue().get()));

    for (Class<?> extClass : IteratorIterable.of(extensionsRanked.stream().map((o) -> o.getKey())
        .iterator())) {
      Extension ext = (Extension) newInstance(extClass);
      extensions.put(extClass, ext);
    }

    Set<Class<?>> extCopy = Collections.unmodifiableSet(extensions.keySet());

    for (Extension ext : extensions.values()) {
      ext.verifyDependencies(this, extCopy);
    }
  }

  final void init(AppHTTPServer server) throws IOException {
    if (!initDone.compareAndSet(false, true)) {
      throw new IllegalStateException("App is already initialized");
    }

    for (Extension ext : extensions.values()) {
      ext.doInit(server);
    }
  }

  /**
   * Initializes {@link Extension} components.
   */
  private final void registerExtensions(LinkedHashMap<Class<?>, AtomicInteger> extensionClasses,
      Class<?> annotatedInClass) {
    LinkedHashMap<Class<? extends Extension>, AtomicInteger> foundExtensions = Component
        .getAnnotatedExtensions(annotatedInClass);
    if (foundExtensions.isEmpty()) {
      return;
    }

    // BaseSupport is implied.
    foundExtensions.computeIfAbsent(BaseSupport.class, (key) -> new AtomicInteger(0)).addAndGet(
        foundExtensions.size());

    for (Map.Entry<Class<? extends Extension>, AtomicInteger> en : foundExtensions.entrySet()) {
      extensionClasses.computeIfAbsent(en.getKey(), (key) -> new AtomicInteger(0)).addAndGet(en
          .getValue().intValue());
      registerExtensions(extensionClasses, en.getKey());
    }
  }

  private final List<Closeable> closeables = Collections.synchronizedList(
      new ArrayList<Closeable>());

  /**
   * Registers the designated RPC services.
   *
   * @param registry The target RPC registry.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  final void initRPC(RPCRegistry registry) {
    this.rpcRegistry = registry;

    registry.registerRPCService(ConsoleService.class, new ConsoleService() {

      @Override
      public Object requestNextChunk() {
        DumboSession session = DumboSession.getSession();

        return ((ConsoleImpl) session.getConsole()).getConsoleService().requestNextChunk();
      }
    });

    LinkedHashSet<Class<?>> serviceClasses = new LinkedHashSet<>();
    for (Class<?> extClass : extensions.keySet()) {
      serviceClasses.addAll(getAnnotatedServices(extClass));
    }
    for (Class<?> serviceClass : serviceClasses) {
      Class<?>[] interfaces = serviceClass.getInterfaces();
      registry.registerRPCService((Class) interfaces[0], newInstance(serviceClass));
    }
    onRPCInit(registry);
  }

  protected void onRPCInit(RPCRegistry registry) {
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
  protected void onAppLoaded(final DumboSession session) {
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
        LOG.info("Error while closing object of type " + cl.getClass(), e);
      }
    }
    closed = true;
  }

  /**
   * Called when the application has been quit, for example when all browser windows have been
   * closed.
   */
  protected void onQuit() {
    try {
      close();
    } catch (IOException e) {
      LOG.info("Could not close app", e);
    }
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
  Collection<Extension> getExtensions() {
    return extensions.values();
  }

  @SuppressWarnings("unchecked")
  <T extends Extension> T getExtension(Class<T> clazz) {
    return (T) extensions.get(clazz);
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

  /**
   * Look up a resource in the resource path of the app and any registered {@link Extension}.
   *
   * @param path The path to look up (usually relative).
   * @return The {@link URL} pointing to the resource, or {@code null} if not found/not accessible.
   */
  public URL getResource(String path) {
    URL resource = getClass().getResource(path);
    if (resource != null) {
      return resource;
    }
    for (Extension ext : getExtensions()) {
      resource = ext.getResource(path);
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
  protected final void onStart() {
    new Thread() {
      @Override
      public void run() {
        try {
          onAppStart();
        } catch (Exception e) {
          LOG.error("Error upon app start", e);
        }
      };
    }.start();
  }

  /**
   * This method will be called upon application start.
   */
  protected void onAppStart() {
  }
}
