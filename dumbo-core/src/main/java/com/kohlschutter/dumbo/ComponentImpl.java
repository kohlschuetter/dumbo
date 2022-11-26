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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.Nullable;

import com.kohlschutter.dumbo.annotations.Services;
import com.kohlschutter.dumbo.annotations.Servlets;
import com.kohlschutter.dumbo.util.AnnotationUtil;

/**
 * A component is something that can have {@link Services}, {@link Components} and {@link Servlets}
 * annotations
 *
 * @author Christian Kohlschütter
 */
public abstract class ComponentImpl implements Component {
  private final Class<? extends Component> componentClass;

  private Set<Class<?>> reachableExtensions = null;

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

  static LinkedHashMap<Class<? extends Component>, AtomicInteger> getAnnotatedComponents(
      Class<?> fromClass) {
    return AnnotationUtil.getClassesFromAnnotationWithCount(fromClass, Components.class,
        Component.class);
  }

  static Set<Class<? extends Object>> getAnnotatedServices(Class<?> fromClass) {
    return AnnotationUtil.getClassesFromAnnotationWithCount(fromClass, Services.class, Object.class)
        .keySet();
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

    if (reachableExtensions == null) {
      reachableExtensions = new HashSet<>();
      reachableExtensions.add(componentClass);
      reachableExtensions.add(BaseSupport.class);
      for (Class<?> ext : AnnotationUtil.getClassesFromAnnotationWithCount(componentClass,
          Components.class, ExtensionImpl.class).keySet()) {
        reachableExtensions.add(ext);
      }
    }

    LinkedHashSet<T> annotations = new LinkedHashSet<>();
    for (Class<?> klazz : reachableExtensions) {
      annotations.addAll(AnnotationUtil.getAnnotations(klazz, annotationClass));
    }
    return annotations;
  }
}
