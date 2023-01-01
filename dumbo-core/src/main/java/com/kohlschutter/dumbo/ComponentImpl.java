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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import com.kohlschutter.dumbo.api.DumboComponent;
import com.kohlschutter.dumbo.exceptions.ExtensionDependencyException;
import com.kohlschutter.dumbo.util.AnnotationUtil;

class ComponentImpl implements BaseSupport {

  private final AtomicBoolean initialized = new AtomicBoolean();

  private final Class<? extends DumboComponent> componentClass;

  private LinkedHashSet<Class<?>> reachableComponents = null;

  protected ComponentImpl(Class<? extends DumboComponent> compClass) {
    this.componentClass = compClass == null ? getClass() : compClass;
  }

  protected Class<? extends DumboComponent> getComponentClass() {
    return componentClass;
  }

  /**
   * Retrieves a list of reachable {@link DumboComponent}s.
   *
   * @param extra An additional class to retrieve {@link DumboComponent}s from.
   * @return A collection of {@link DumboComponent} instances, in dependency order.
   */
  protected synchronized LinkedHashSet<Class<?>> getReachableComponents() {
    if (reachableComponents == null) {
      reachableComponents = new LinkedHashSet<>();
      reachableComponents.add(BaseSupport.class);
      reachableComponents.add(componentClass);
      reachableComponents.addAll(linearizeComponentHierarchy(componentClass));
    }

    return reachableComponents;
  }

  <T extends Annotation> LinkedHashSet<T> getAnnotations(Class<T> annotationClass) {
    LinkedHashSet<T> set = new LinkedHashSet<>();
    for (Class<?> comp : getReachableComponents()) {
      T ann = comp.getAnnotation(annotationClass);
      if (ann != null) {
        set.add(ann);
      }
    }
    return set;
  }

  <T extends Annotation> @Nullable T getMostRecentComponentAnnotation(Class<T> annotationClass) {
    List<T> annotations = getComponentAnnotations(annotationClass);
    if (annotations.isEmpty()) {
      return null;
    } else {
      return annotations.get(annotations.size() - 1);
    }
  }

  <T extends Annotation> List<T> getComponentAnnotations(Class<T> annotationClass) {
    return AnnotationUtil.getAnnotations(componentClass, annotationClass);
  }

  final <T extends Annotation> LinkedHashSet<T> getAnnotatedMappingsFromAllReachableComponents(
      Class<T> annotationClass) {
    LinkedHashSet<T> annotations = new LinkedHashSet<>();
    for (Class<?> klazz : getReachableComponents()) {
      annotations.addAll(AnnotationUtil.getAnnotations(klazz, annotationClass));
    }
    return annotations;
  }

  private static Collection<Class<?>> linearizeComponentHierarchy(Class<?> leafClass) {
    LinkedHashMap<Class<?>, AtomicInteger> countMap = new LinkedHashMap<>();

    traverseComponentHierarchy(leafClass, countMap);

    List<Map.Entry<Class<?>, AtomicInteger>> list = new ArrayList<>(countMap.entrySet());
    list.sort((a, b) -> b.getValue().get() - a.getValue().get());

    return list.stream().map((e) -> e.getKey()).collect(Collectors.toList());
  }

  private static void traverseComponentHierarchy(Class<?> clazzToInspect,
      LinkedHashMap<Class<?>, AtomicInteger> countMap) {
    if (!DumboComponent.class.isAssignableFrom(clazzToInspect)) {
      return;
    }

    Class<?> superClass = clazzToInspect.getSuperclass();
    if (superClass != null) {
      traverseComponentHierarchy(superClass, countMap);
    }

    for (Class<?> intf : clazzToInspect.getInterfaces()) {
      traverseComponentHierarchy(intf, countMap);
    }

    countMap.computeIfAbsent(clazzToInspect, (k) -> new AtomicInteger(0)).incrementAndGet();
  }

  URL getComponentResource(String name) {
    return getComponentClass().getResource(name);
  }

  /**
   * Performs dependency checks.
   *
   * @param app The server app
   * @param extensions The extensions to check.
   * @throws ExtensionDependencyException on dependency conflict.
   */
  void verifyDependencies(final ServerApp app, Set<Class<?>> extensions)
      throws ExtensionDependencyException {
  }

  /**
   * Called by the app to initialize the {@link ExtensionImpl} for the given {@link AppHTTPServer}.
   *
   * @throws IOException on error.
   */
  void initComponent(AppHTTPServer app) throws IOException {
    if (!initialized.compareAndSet(false, true)) {
      throw new IllegalStateException("Already initialized");
    }
  }

  @Override
  public String toString() {
    return super.toString() + "<" + getComponentClass() + ": " + getReachableComponents() + ">";
  }
}
