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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import com.kohlschutter.dumbo.annotations.Component;
import com.kohlschutter.dumbo.util.AnnotationUtil;

class ComponentImpl implements BaseSupport {
  private final Class<? extends Component> componentClass;

  private LinkedHashSet<Class<?>> reachableComponents = null;

  protected ComponentImpl(Class<? extends Component> compClass) {
    this.componentClass = compClass == null ? getClass() : compClass;
  }

  protected Class<? extends Component> getComponentClass() {
    return componentClass;
  }

  /**
   * Creates a new instance of the given class, trying several construction methods, starting with a
   * constructor that takes this {@link ComponentImpl} as the only parameter.
   *
   * @param <T> The instance type.
   * @param clazz The class to instantiate.
   * @return The instance.
   * @throws IllegalStateException if the class could not be initialized.
   */
  final <T> T newInstance(Class<T> clazz) throws IllegalStateException {
    try {
      try {
        return clazz.getConstructor(ComponentImpl.class).newInstance(this);
      } catch (NoSuchMethodException e) {
        // ignore
      }
      try {
        return clazz.getDeclaredConstructor().newInstance();
      } catch (NoSuchMethodException e) {
        // ignore
      }
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | SecurityException e) {
      throw new IllegalStateException("Could not initialize " + clazz, e);
    }

    throw new IllegalStateException("Could not find a way to initialize " + clazz);
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

  protected LinkedHashSet<Class<?>> getReachableComponents() {
    return getReachableComponents(null);
  }

  LinkedHashSet<Class<?>> getReachableComponents(Class<?> extra) {
    if (reachableComponents == null) {
      reachableComponents = new LinkedHashSet<>();
      reachableComponents.add(BaseSupport.class);
      reachableComponents.add(componentClass);
      reachableComponents.addAll(linearizeComponentHierarchy(componentClass));

      if (extra != null) {
        reachableComponents.add(extra);
        reachableComponents.addAll(linearizeComponentHierarchy(extra));
      }
    }

    return reachableComponents;
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

    for (Map.Entry<Class<?>, AtomicInteger> en : countMap.entrySet()) {
      System.out.println(en.getKey() + ": " + en.getValue());
    }

    List<Map.Entry<Class<?>, AtomicInteger>> list = new ArrayList<>(countMap.entrySet());
    list.sort((a, b) -> b.getValue().get() - a.getValue().get());

    return list.stream().map((e) -> e.getKey()).collect(Collectors.toList());
  }

  private static void traverseComponentHierarchy(Class<?> clazzToInspect,
      LinkedHashMap<Class<?>, AtomicInteger> countMap) {
    if (!Component.class.isAssignableFrom(clazzToInspect)) {
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
}
