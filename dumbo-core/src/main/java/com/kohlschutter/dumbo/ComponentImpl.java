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
import java.util.HashMap;
import java.util.HashSet;
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

  @SuppressWarnings("PMD.LooseCoupling")
  private LinkedHashSet<Class<?>> reachableComponents = null;

  private final Map<Class<? extends DumboComponent>, Set<Class<? extends DumboComponent>>> componentToSubComponents =
      new HashMap<>();

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
  @SuppressWarnings("PMD.LooseCoupling")
  protected synchronized LinkedHashSet<Class<?>> getReachableComponents() {
    if (reachableComponents == null) {
      reachableComponents = new LinkedHashSet<>();
      reachableComponents.add(BaseSupport.class);
      reachableComponents.add(componentClass);
      reachableComponents.addAll(linearizeComponentHierarchy(componentClass,
          componentToSubComponents));
    }

    return reachableComponents;
  }

  @SuppressWarnings("PMD.LooseCoupling")
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

  @SuppressWarnings("PMD.LooseCoupling")
  final <T extends Annotation> LinkedHashSet<T> getAnnotatedMappingsFromAllReachableComponents(
      Class<T> annotationClass) {
    LinkedHashSet<T> annotations = new LinkedHashSet<>();
    for (Class<?> klazz : getReachableComponents()) {
      annotations.addAll(AnnotationUtil.getAnnotations(klazz, annotationClass));
    }
    return annotations;
  }

  @SuppressWarnings("PMD.LooseCoupling")
  private static Collection<Class<?>> linearizeComponentHierarchy(Class<?> leafClass,
      Map<Class<? extends DumboComponent>, Set<Class<? extends DumboComponent>>> componentToSubcomponentsMap) {
    LinkedHashMap<Class<? extends DumboComponent>, AtomicInteger> countMap = new LinkedHashMap<>();

    traverseComponentHierarchy(leafClass, countMap, componentToSubcomponentsMap);

    List<Map.Entry<Class<? extends DumboComponent>, AtomicInteger>> list = new ArrayList<>(countMap
        .entrySet());
    list.sort((a, b) -> b.getValue().get() - a.getValue().get());

    return list.stream().map((e) -> e.getKey()).collect(Collectors.toList());
  }

  @SuppressWarnings("PMD.LooseCoupling")
  private static void traverseComponentHierarchy(Class<?> clazzToInspect0,
      LinkedHashMap<Class<? extends DumboComponent>, AtomicInteger> countMap,
      Map<Class<? extends DumboComponent>, Set<Class<? extends DumboComponent>>> componentToSubcomponentsMap) {
    if (!DumboComponent.class.isAssignableFrom(clazzToInspect0)) {
      return;
    }
    @SuppressWarnings("unchecked")
    Class<? extends DumboComponent> clazzToInspect =
        (Class<? extends DumboComponent>) clazzToInspect0;

    LinkedHashMap<Class<? extends DumboComponent>, AtomicInteger> componentCountMap =
        new LinkedHashMap<>();

    Class<?> superClass = clazzToInspect.getSuperclass();
    if (superClass != null) {
      traverseComponentHierarchy(superClass, countMap, componentToSubcomponentsMap);

      if (componentToSubcomponentsMap != null) {
        traverseComponentHierarchy(superClass, componentCountMap, componentToSubcomponentsMap);
      }
    }

    for (Class<?> intf : clazzToInspect.getInterfaces()) {
      traverseComponentHierarchy(intf, countMap, componentToSubcomponentsMap);
      if (componentToSubcomponentsMap != null) {
        traverseComponentHierarchy(intf, componentCountMap, componentToSubcomponentsMap);
      }
    }

    countMap.computeIfAbsent(clazzToInspect, (k) -> new AtomicInteger(0)).incrementAndGet();
    if (!DumboComponent.class.equals(clazzToInspect)) {
      componentCountMap.computeIfAbsent(clazzToInspect, (k) -> new AtomicInteger(0))
          .incrementAndGet();
    }

    if (!componentCountMap.isEmpty() && componentToSubcomponentsMap != null) {
      componentToSubcomponentsMap.put(clazzToInspect, new HashSet<Class<? extends DumboComponent>>(
          componentCountMap.keySet()));
    }
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
   * Called by the app to initialize the {@link ExtensionImpl} for the given
   * {@link DumboServerImpl}.
   *
   * @throws IOException on error.
   */
  void initComponent(DumboServerImpl app) throws IOException {
    if (!initialized.compareAndSet(false, true)) {
      throw new IllegalStateException("Already initialized");
    }
  }

  @Override
  public String toString() {
    return super.toString() + "<" + getComponentClass() + ": " + getReachableComponents() + ">";
  }

  Map<Class<? extends DumboComponent>, Set<Class<? extends DumboComponent>>> getComponentToSubComponentsMap() {
    return componentToSubComponents;
  }
}
