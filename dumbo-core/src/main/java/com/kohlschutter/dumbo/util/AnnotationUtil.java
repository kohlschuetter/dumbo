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
package com.kohlschutter.dumbo.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.kohlschutter.dumbo.Component;
import com.kohlschutter.dumbo.ext.Extensions;
import com.kohlschutter.dumbo.ext.Services;

public final class AnnotationUtil {
  private AnnotationUtil() {
    throw new IllegalStateException("No instances");
  }

  @SuppressWarnings("unchecked")
  public static final <T> LinkedHashMap<Class<? extends T>, AtomicInteger> getClassesFromAnnotationWithCount(
      Class<?> leafClass, Class<? extends Annotation> annotationClass, Class<T> basicType) {

    ListIterator<Class<?>> it = prevListIterator(leafClass, annotationClass);
    LinkedHashMap<Class<? extends T>, AtomicInteger> list = new LinkedHashMap<>();
    while (it.hasPrevious()) {
      Class<?> classToInspect = it.previous();

      Annotation annotation = Objects.requireNonNull(classToInspect.getAnnotation(annotationClass));

      final Class<?>[] value;
      if (annotation instanceof Services) {
        value = ((Services) annotation).value();
      } else if (annotation instanceof Extensions) {
        value = ((Extensions) annotation).value();
      } else {
        throw new IllegalStateException("Unsupported annotation type: " + annotationClass);
      }

      for (Class<?> en : value) {
        list.computeIfAbsent((Class<T>) en, (k) -> new AtomicInteger(0)).incrementAndGet();
      }
    }

    return list;
  }

  public static <T extends Annotation> List<T> getAnnotations(Class<?> leafClass,
      Class<T> annotationClass) {
    ListIterator<Class<?>> it = prevListIterator(leafClass, annotationClass);
    List<T> list = new ArrayList<>();
    while (it.hasPrevious()) {
      Class<?> classToInspect = it.previous();

      T annotation = Objects.requireNonNull(classToInspect.getAnnotation(annotationClass));

      list.add(annotation);
    }
    return list;
  }

  private static ListIterator<Class<?>> prevListIterator(Class<?> leafClass,
      Class<? extends Annotation> annotationClass) {
    List<Class<?>> classesToInspect = new ArrayList<>();

    Class<?> candidateClass = leafClass;
    do {
      if (candidateClass.isAnnotationPresent(annotationClass)) {
        classesToInspect.add(candidateClass);
      }
      if (Component.class.equals(candidateClass)) {
        break;
      }
      candidateClass = candidateClass.getSuperclass();
    } while (candidateClass != null && candidateClass != Object.class);

    return classesToInspect.listIterator(classesToInspect.size());
  }
}
