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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.annotations.EventHandlers;
import com.kohlschutter.dumbo.annotations.Services;
import com.kohlschutter.dumbo.api.Application;
import com.kohlschutter.dumbo.api.Component;
import com.kohlschutter.dumbo.api.DumboSession;
import com.kohlschutter.dumbo.api.EventHandler;
import com.kohlschutter.dumbo.console.ConsoleService;
import com.kohlschutter.dumbo.exceptions.ExtensionDependencyException;

/**
 * Internal base class for a lightweight Server-based application.
 */
public final class ServerApp implements Closeable, Cloneable {
  private static final Logger LOG = LoggerFactory.getLogger(ServerApp.class);

  private final LinkedHashMap<Class<?>, ExtensionImpl> extensions = new LinkedHashMap<>();
  private final Map<Class<?>, Object> instances = new HashMap<>();
  private final List<EventHandler> eventHandlers = new ArrayList<>();

  private AtomicBoolean initDone = new AtomicBoolean(false);
  private volatile boolean closed = false;
  private RPCRegistry rpcRegistry;

  private boolean staticDesignMode = false;

  private final Class<? extends Application> applicationClass;
  private final ComponentImpl applicationComponentImpl;

  public ServerApp(Class<? extends Application> applicationClass) {
    this.applicationClass = applicationClass;
    this.applicationComponentImpl = new ComponentImpl(applicationClass);
    resolveExtensions(applicationClass);

    initEventHandlers();
  }

  private void resolveExtensions(Class<? extends Component> mainComponent)
      throws ExtensionDependencyException {
    LinkedHashSet<Class<?>> reachableComponents = applicationComponentImpl.getReachableComponents();

    for (Class<?> compClass : reachableComponents) {
      @SuppressWarnings("unchecked")
      ExtensionImpl ext = new ExtensionImpl((Class<? extends Component>) compClass);
      extensions.put(compClass, ext);
    }

    for (ExtensionImpl ext : extensions.values()) {
      ext.verifyDependencies(this, extensions.keySet());
    }
  }

  private void initEventHandlers() {
    List<Class<? extends EventHandler>> eventHandlerClasses = applicationComponentImpl
        .getAnnotations(EventHandlers.class).stream().map((s) -> s.value()).flatMap(Stream::of)
        .distinct().collect(Collectors.toList());

    for (Class<? extends EventHandler> c : eventHandlerClasses) {
      eventHandlers.add(getInstance(c));
    }
  }

  final void init(AppHTTPServer server) throws IOException {
    if (!initDone.compareAndSet(false, true)) {
      throw new IllegalStateException("App is already initialized");
    }

    for (ExtensionImpl ext : extensions.values()) {
      ext.doInit(server);
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
        DumboSession session = DumboSessionImpl.getSession();

        return ((ConsoleImpl) session.getConsole()).getConsoleService().requestNextChunk();
      }
    });

    List<Class<?>> serviceClasses = applicationComponentImpl.getAnnotations(Services.class).stream()
        .map((s) -> s.value()).flatMap(Stream::of).distinct().collect(Collectors.toList());
    for (Class<?> serviceClass : serviceClasses) {
      Class<?>[] interfaces = serviceClass.getInterfaces();
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

  public Class<? extends Application> getApplicationClass() {
    return applicationClass;
  }

  ComponentImpl getApplicationComponentImpl() {
    return applicationComponentImpl;
  }

  /**
   * Look up a resource in the resource path of the app and any registered {@link ExtensionImpl}.
   *
   * @param path The path to look up (usually relative).
   * @return The {@link URL} pointing to the resource, or {@code null} if not found/not accessible.
   */
  public URL getResource(String path) {
    URL resource = applicationComponentImpl.getComponentClass().getResource(path);
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

  /**
   * This method will be called upon application start.
   */
  protected void onAppStart() {
  }
}
