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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.log4j.Logger;

import com.kohlschutter.dumbo.console.ConsoleService;

/**
 * Base class for a lightweight Server-based application.
 */
public abstract class ServerApp implements AppControlService, Closeable, Cloneable {
  private static final Logger LOG = Logger.getLogger(ServerApp.class);
  private final Set<Extension> extensions = new LinkedHashSet<Extension>();
  private boolean initExtensionsDone = false;
  private volatile boolean registeredExtension = false;
  private volatile boolean closed = false;
  private RPCRegistry rpcRegistry;

  protected void initRPC(final RPCRegistry registry) {
  }

  /**
   * Initializes internal {@link Extension} components that are usually required for any app.
   */
  protected void initExtensionsInternal() {
    registerExtension(new BaseSupport());
  }

  final void initInternal() throws IOException {
    if (initExtensionsDone) {
      return;
    }

    initExtensionsInternal();
    initExtensions();

    DEPENDENCY_LOOP : do {
      registeredExtension = false;

      List<Extension> extCopy = Collections.unmodifiableList(new ArrayList<Extension>(extensions));
      for (Extension ext : extCopy) {
        ext.resolveDependencies(this, extCopy);
        if (registeredExtension) {
          continue DEPENDENCY_LOOP;
        }
      }
    } while (registeredExtension);
    initExtensionsDone = true;
  }

  /**
   * Initializes internal {@link Extension} components that are specific to your app.
   * 
   * Use {@link #registerExtension(Extension)} to add an extension.
   */
  protected abstract void initExtensions();

  /**
   * Registers an {@link Extension} to be used with this app.
   * 
   * @param ext The extension to register.
   * @throws IllegalStateException if the call was made outside of {@link #initExtensions()}, or if
   *           an extension was already registered.
   */
  public final void registerExtension(final Extension ext) {
    if (initExtensionsDone) {
      throw new IllegalStateException("Can no longer register extensions at this point.");
    }
    if (extensions.add(ext)) {
      registeredExtension = true;
    }
  }

  private final List<Closeable> closeables = Collections.synchronizedList(
      new ArrayList<Closeable>());

  /**
   * Registers the default RPC services.
   * 
   * @param registry The target RPC registry.
   */
  final void initRPCInternal(RPCRegistry registry) {
    this.rpcRegistry = registry;
    registry.registerRPCService(AppControlService.class, this);

    registry.registerRPCService(ConsoleService.class, new ConsoleService() {

      @Override
      public Object requestNextChunk(String appId) {
        DumboSession session = DumboSession.getSessionIfExists();
        if (session == null) {
          return null;
        }

        return ((ConsoleImpl) session.getConsole()).getConsoleService().requestNextChunk(appId);
      }
    });
  }

  private boolean staticDesignMode = false;

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

  @Override
  public final String notifyAppLoaded() {
    DumboSession session = DumboSession.getSessionIfExists();
    if (session == null) {
      // ignore
      return null;
    }
    String pageId = session.getPageId();

    try {
      onAppLoaded(session);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return pageId;
  }

  /**
   * Called when an instance of this app (e.g., a browser window) has loaded.
   * 
   * @param session The session for this instance.
   */
  protected void onAppLoaded(final DumboSession session) {
  }

  /**
   * Called when an instance of this app (e.g., a browser window) is being unloaded/closed or
   * reloaded.
   * 
   * @param pageId The app ID for this instance.
   */
  protected void onAppUnload(final String pageId) {
  }

  @Override
  public final void notifyAppUnload(final String pageId) {
    onAppUnload(pageId);

    DumboSession.removePageIdFromCurrentSession(pageId);
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
  public Collection<Extension> getExtensions() {
    return extensions;
  }

  /**
   * Returns a clone of this app, with all runtime-specific attributes reset.
   */
  @Override
  public ServerApp clone() {
    try {
      ServerApp clone = (ServerApp) super.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException(e);
    }
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

  /**
   * Called after initialization, unless static design mode is enabled.
   * 
   * This is the place where you want to start the "main" operations.
   * 
   * @see #initRPC(RPCRegistry)
   */
  protected void onStart() {
  }

  RPCRegistry getRPCRegistry() {
    return rpcRegistry;
  }
}
