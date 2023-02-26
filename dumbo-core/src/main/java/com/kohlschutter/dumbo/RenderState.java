package com.kohlschutter.dumbo;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.kohlschutter.dumbo.api.DumboComponent;

/**
 * The render state of a dumbo page.
 * 
 * @author Christian Kohlschütter
 */
public final class RenderState {
  private static final ThreadLocal<RenderState> THREADLOCAL = new ThreadLocal<>() {

    @Override
    protected RenderState initialValue() {
      return new RenderState();
    }
  };

  private boolean componentsMarkedUseAll = false;
  private final Set<Class<? extends DumboComponent>> componentsMarkedUsed = new HashSet<>();
  private final Set<String> included = new LinkedHashSet<>();
  private String relativePath;

  public static ThreadLocal<RenderState> getThreadLocal() {
    return THREADLOCAL;
  }

  public static RenderState get() {
    return getThreadLocal().get();
  }

  public Set<String> getIncluded() {
    return included;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  public boolean isMarkedUsed(Class<? extends DumboComponent> componentClass) {
    return componentsMarkedUseAll || componentsMarkedUsed.contains(componentClass);
  }

  public void setMarkedUsed(Class<? extends DumboComponent> componentClass) {
    componentsMarkedUsed.add(componentClass);
  }

  /**
   * Marks all optional components as "used". This may be necessary for JSP pages, etc.
   */
  public void setMarkedUsedAllComponents(boolean allUsed) {
    this.componentsMarkedUseAll = allUsed;
  }
}
