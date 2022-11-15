package com.kohlschutter.dumbo;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;

import com.kohlschutter.dumbo.console.Console;

import jakarta.servlet.http.HttpSession;

public final class DumboSession {
  private static final String SESSION_ATTRIBUTE_PAGEIDS = "com.kohlschutter.dumbo.PageIds";
  private static final ThreadLocal<DumboSession> TL_DUMBO_SESSION = new ThreadLocal<>();
  private final String pageId;
  private final Map<String, Object> pageScope = new HashMap<>();
  private final HttpSession context;
  private final Console console = new ConsoleImpl();
  private AtomicBoolean invalid = new AtomicBoolean(false);

  DumboSession(String pageId, HttpSession context) {
    this.pageId = pageId;
    this.context = context;
  }

  public static DumboSession getSession() {
    DumboSession session = getSessionIfExists();
    if (session == null) {
      throw new NoSessionException();
    } else {
      return session;
    }
  }

  public static DumboSession getSessionIfExists() {
    return TL_DUMBO_SESSION.get();
  }

  public static String getCurrentPageId() {
    DumboSession session = getSessionIfExists();
    if (session == null) {
      return null;
    } else {
      return session.getPageId();
    }
  }

  public static Set<String> getCurrentPageIds() {
    DumboSession session = getSessionIfExists();
    if (session == null) {
      return Collections.emptySet();
    } else {
      return session.getPageIds();
    }
  }

  public Set<String> getPageIds() {
    return getDumboSessionPageIds(context);
  }

  static void setSessionTL(DumboSession session) {
    TL_DUMBO_SESSION.set(session);
  }

  static void removeSessionTL() {
    TL_DUMBO_SESSION.remove();
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

  public <@NonNull T> T getOrCreatePageAttribute(Class<T> key, Supplier<T> attrSupplier) {
    return getOrCreatePageAttribute(key.getName(), attrSupplier);
  }

  public <@NonNull T> T getOrCreatePageAttribute(Class<T> key, Function<String, T> attrSupplier) {
    return getOrCreatePageAttribute(key.getName(), attrSupplier);
  }

  @SuppressWarnings("unchecked")
  public <@NonNull T> T getOrCreatePageAttribute(String key, Supplier<T> attrSupplier) {
    checkValid();
    synchronized (pageScope) {
      return (T) pageScope.computeIfAbsent(key, (k) -> attrSupplier.get());
    }
  }

  @SuppressWarnings("unchecked")
  public <@NonNull T> T getOrCreatePageAttribute(String key, Function<String, T> attrSupplier) {
    checkValid();
    synchronized (pageScope) {
      return (T) pageScope.computeIfAbsent(key, attrSupplier);
    }
  }

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
      Set<String> existingPageIds = DumboSession.getDumboSessionPageIds(context);
      int toDelete = Math.max(0, existingPageIds.size() + 1 - max);
      if (toDelete > 0) {
        for (String p : existingPageIds) {
          DumboSession.removePageId(context, p);
          if (--toDelete <= 0) {
            break;
          }
        }
      }
    }

    String pageId = UUID.randomUUID().toString();
    synchronized (context) {
      @SuppressWarnings("unchecked")
      Map<String, DumboSession> map = (Map<String, DumboSession>) context.getAttribute(
          SESSION_ATTRIBUTE_PAGEIDS);
      if (map == null) {
        map = new LinkedHashMap<>();
        context.setAttribute(SESSION_ATTRIBUTE_PAGEIDS, map);
      }
      map.put(pageId, new DumboSession(pageId, context));
    }
    return pageId;
  }

  static Set<String> getDumboSessionPageIds(HttpSession context) {
    synchronized (context) {
      @SuppressWarnings("unchecked")
      Map<String, DumboSession> map = (Map<String, DumboSession>) context.getAttribute(
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

  static DumboSession getDumboSession(HttpSession context, String pageId) {
    synchronized (context) {
      @SuppressWarnings("unchecked")
      Map<String, DumboSession> map = (Map<String, DumboSession>) context.getAttribute(
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
    synchronized (context) {
      @SuppressWarnings("unchecked")
      Map<String, DumboSession> map = (Map<String, DumboSession>) context.getAttribute(
          SESSION_ATTRIBUTE_PAGEIDS);
      if (map == null) {
        return;
      } else {
        DumboSession session = map.remove(pageId);
        if (map.isEmpty()) {
          context.removeAttribute(SESSION_ATTRIBUTE_PAGEIDS);
        }
        if (session != null) {
          session.invalidate();
        }
      }
    }
  }

  private void checkValid() {
    if (invalid.get()) {
      throw new InvalidSessionException();
    }
  }

  void invalidate() {
    if (invalid.compareAndSet(false, true)) {
      console.shutdown();
    }
  }

  static void removePageIdFromCurrentSession(String pageId) {
    DumboSession session = getSessionIfExists();
    if (session == null) {
      return;
    }
    removePageId(session.context, pageId);
  }

  /**
   * Returns the console.
   * 
   * @return The console.
   */
  public Console getConsole() {
    checkValid();
    return console;
  }
}
