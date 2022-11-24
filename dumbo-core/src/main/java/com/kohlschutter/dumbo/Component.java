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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.kohlschutter.dumbo.util.AnnotationUtil;

/**
 * A component is something that can have {@link Services}, {@link Extensions} and {@link Servlets}
 * annotations
 *
 * @author Christian Kohlschütter
 */
public abstract class Component {
  private Set<Class<?>> reachableExtensions = null;

  protected Component() {
  }

  protected <T> T newInstance(Class<T> clazz) {
    try {
      try {
        return clazz.getConstructor(Component.class).newInstance(this);
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
      e.printStackTrace();
    }

    throw new IllegalStateException("Could not find a way to initialize " + clazz);
  }

  static LinkedHashMap<Class<? extends Extension>, AtomicInteger> getAnnotatedExtensions(
      Class<?> fromClass) {
    return AnnotationUtil.getClassesFromAnnotationWithCount(fromClass, Extensions.class,
        Extension.class);
  }

  static Set<Class<? extends Object>> getAnnotatedServices(Class<?> fromClass) {
    return AnnotationUtil.getClassesFromAnnotationWithCount(fromClass, Services.class, Object.class)
        .keySet();
  }

  protected final <T extends Annotation> Collection<T> getAnnotatedMappings(
      Class<T> annotationClass) {
    synchronized (this) {
      if (reachableExtensions == null) {
        reachableExtensions = new HashSet<>();
        reachableExtensions.add(getClass());
        reachableExtensions.add(BaseSupport.class);
        for (Class<?> ext : AnnotationUtil.getClassesFromAnnotationWithCount(getClass(),
            Extensions.class, Extension.class).keySet()) {
          reachableExtensions.add(ext);
        }
      }
    }

    List<T> annotations = new ArrayList<>();
    for (Class<?> klazz : reachableExtensions) {
      annotations.addAll(AnnotationUtil.getAnnotations(klazz, annotationClass));
    }
    return annotations;
  }
}
