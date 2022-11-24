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
