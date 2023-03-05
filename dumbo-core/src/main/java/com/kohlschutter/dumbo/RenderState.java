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

  private ServerApp app;
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

  public ServerApp getApp() {
    return app;
  }

  public void setApp(ServerApp app) {
    this.app = app;
  }
}
