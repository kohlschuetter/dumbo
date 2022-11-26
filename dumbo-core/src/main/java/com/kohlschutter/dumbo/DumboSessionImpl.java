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

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;

import com.kohlschutter.dumbo.annotations.Console;
import com.kohlschutter.dumbo.annotations.DumboSession;
import com.kohlschutter.dumbo.exceptions.InvalidSessionException;

import jakarta.servlet.http.HttpSession;

public final class DumboSessionImpl extends DumboSession {
  private static final String SESSION_ATTRIBUTE_PAGEIDS = "com.kohlschutter.dumbo.PageIds";
  private final String pageId;
  private final Map<String, Object> pageScope = new HashMap<>();
  private final HttpSession context;
  private final Console console = new ConsoleImpl(this);
  private AtomicBoolean invalid = new AtomicBoolean(false);

  DumboSessionImpl(String pageId, HttpSession context) {
    this.pageId = pageId;
    this.context = context;
  }

  public Set<String> getPageIds() {
    return getDumboSessionPageIds(context);
  }

  public String getPageId() {
    return pageId;
  }

  public String getSessionId() {
    return context.getId();
  }

  public Object getPageAttribute(String key) {
    checkValid();
    synchronized (pageScope) {
      return pageScope.get(key);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <@NonNull T> T getOrCreatePageAttribute(String key, Function<String, T> attrSupplier) {
    checkValid();
    synchronized (pageScope) {
      return (T) pageScope.computeIfAbsent(key, attrSupplier);
    }
  }

  @Override
  public void setPageAttribute(String key, Object val) {
    checkValid();
    synchronized (pageScope) {
      pageScope.put(key, val);
    }
  }

  public Object getSessionAttribute(String key) {
    checkValid();
    return context.getAttribute("dumbo." + key);
  }

  public void setSessionAttribute(String key, Object val) {
    checkValid();
    context.setAttribute("dumbo." + key, val);
  }

  static String newPageId(HttpSession context, int max) {
    if (max > 0) {
      Set<String> existingPageIds = DumboSessionImpl.getDumboSessionPageIds(context);
      int toDelete = Math.max(0, existingPageIds.size() + 1 - max);
      if (toDelete > 0) {
        for (String p : existingPageIds) {
          DumboSessionImpl.removePageId(context, p);
          if (--toDelete <= 0) {
            break;
          }
        }
      }
    }

    String pageId = UUID.randomUUID().toString();
    synchronized (context) {
      @SuppressWarnings("unchecked")
      Map<String, DumboSessionImpl> map = (Map<String, DumboSessionImpl>) context.getAttribute(
          SESSION_ATTRIBUTE_PAGEIDS);
      if (map == null) {
        map = new LinkedHashMap<>();
        context.setAttribute(SESSION_ATTRIBUTE_PAGEIDS, map);
      }
      map.put(pageId, new DumboSessionImpl(pageId, context));
    }
    return pageId;
  }

  static Set<String> getDumboSessionPageIds(HttpSession context) {
    synchronized (context) {
      @SuppressWarnings("unchecked")
      Map<String, DumboSessionImpl> map = (Map<String, DumboSessionImpl>) context.getAttribute(
          SESSION_ATTRIBUTE_PAGEIDS);
      if (map == null) {
        return Collections.emptySet();
      } else {
        switch (map.size()) {
          case 0:
            return Collections.emptySet();
          case 1:
            return Collections.singleton(map.keySet().iterator().next());
          default:
            return new LinkedHashSet<>(map.keySet());
        }
      }
    }
  }

  static DumboSessionImpl getDumboSession(HttpSession context, String pageId) {
    synchronized (context) {
      @SuppressWarnings("unchecked")
      Map<String, DumboSessionImpl> map = (Map<String, DumboSessionImpl>) context.getAttribute(
          SESSION_ATTRIBUTE_PAGEIDS);
      if (map == null) {
        return null;
      } else {
        return map.get(pageId);
      }
    }
  }

  static void removePageId(HttpSession context, String pageId) {
    if (pageId == null) {
      return;
    }
    DumboSessionImpl session;
    synchronized (context) {
      @SuppressWarnings("unchecked")
      Map<String, DumboSessionImpl> map = (Map<String, DumboSessionImpl>) context.getAttribute(
          SESSION_ATTRIBUTE_PAGEIDS);
      if (map == null) {
        return;
      }
      session = map.remove(pageId);
      if (session == null) {
        return;
      } else if (map.isEmpty()) {
        context.removeAttribute(SESSION_ATTRIBUTE_PAGEIDS);

        AppHTTPServer server = (AppHTTPServer) context.getServletContext().getAttribute(
            AppHTTPServer.class.getName());
        if (server != null) {
          server.onSessionShutdown(context.getId(), new WeakReference<>(context));
        }
      }
    }
    if (session != null) {
      session.invalidate();
    }
  }

  private void checkValid() {
    if (invalid.get()) {
      throw new InvalidSessionException();
    }
  }

  @Override
  public void invalidate() {
    if (invalid.compareAndSet(false, true)) {
      console.shutdown();
    }
    removePageIdFromCurrentSession(pageId);
  }

  static void removePageIdFromCurrentSession(String pageId) {
    DumboSession session = getSessionIfExists();
    if (session == null) {
      return;
    }
    removePageId(((DumboSessionImpl) session).context, pageId);
  }

  /**
   * Returns the console.
   *
   * @return The console.
   */
  @Override
  public Console getConsole() {
    checkValid();
    return console;
  }
}
