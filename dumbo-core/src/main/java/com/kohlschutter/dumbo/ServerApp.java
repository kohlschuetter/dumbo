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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Internal base class for a lightweight Server-based application that is configurable using Java
 * annotations
 */
@Extensions({})
public abstract class ServerApp extends ServerAppBase {
  private static final Logger LOG = Logger.getLogger(ServerApp.class);

  private final Set<Class<Extension>> annotatedExtensions;
  private final Set<Class<Object>> annotatedServices;

  protected ServerApp() {
    super();

    this.annotatedExtensions = new LinkedHashSet<>(getClassesFromAnnotation(Extensions.class,
        Extension.class));
    this.annotatedServices = new LinkedHashSet<>(getClassesFromAnnotation(Services.class,
        Object.class));
  }

  @SuppressWarnings("unchecked")
  @Override
  protected final void initRPC(final RPCRegistry registry) {
    for (Class<Object> extClass : annotatedServices) {
      Class<?>[] interfaces = extClass.getInterfaces();
      registry.registerRPCService((Class<Object>) interfaces[0], newInstance(extClass));
    }
    onRPCInit(registry);
  }

  protected void onRPCInit(RPCRegistry registry) {
  }

  @Override
  protected final void initExtensions() {
    for (Class<Extension> extClass : annotatedExtensions) {
      registerExtension(newInstance(extClass));
    }
  }

  /**
   * Look up a resource in the resource path of the app and any registered {@link Extension}.
   * 
   * @param path The path to look up (usually relative).
   * @return The {@link URL} pointing to the resource, or {@code null} if not found/not accessible.
   */
  public URL getResource(String path) {
    URL resource = getClass().getResource(path);
    if (resource != null) {
      return resource;
    }
    for (Class<Extension> extClass : annotatedExtensions) {
      resource = extClass.getResource(path);
      if (resource != null) {
        return resource;
      }
    }
    return null;
  }

  private <T> T newInstance(Class<T> clazz) {
    try {
      try {
        return clazz.getConstructor(ServerAppBase.class).newInstance(this);
      } catch (NoSuchMethodException e) {
      }
      try {
        clazz.getConstructor();
        return clazz.getDeclaredConstructor().newInstance();
      } catch (NoSuchMethodException e) {
      }
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | SecurityException e) {
      LOG.info(e);
    }

    throw new IllegalStateException("Could not find a way to initialize " + clazz);
  }

  @SuppressWarnings("unchecked")
  protected final <T> List<Class<T>> getClassesFromAnnotation(
      Class<? extends Annotation> annotationClass, Class<T> basicType) {

    List<Class<T>> list = new LinkedList<>();

    Class<?> clazz = getClass();

    do {
      if (clazz.isAnnotationPresent(annotationClass)) {
        Annotation annotation = clazz.getAnnotation(annotationClass);

        final Class<?>[] value;
        if (annotation instanceof Services) {
          value = ((Services) annotation).value();
        } else if (annotation instanceof Extensions) {
          value = ((Extensions) annotation).value();
        } else {
          throw new IllegalStateException("Unsupported annotation type: " + annotationClass);
        }

        for (Class<?> en : value) {
          list.add(0, (Class<T>) en);
        }
      }

      if (clazz == ServerApp.class) {
        break;
      }

      clazz = clazz.getSuperclass();
    } while (clazz != null && clazz != Object.class);

    return list;
  }

  @Override
  protected final void onStart() {
    super.onStart();
    new Thread() {
      @Override
      public void run() {
        try {
          onAppStart();
        } catch (Exception e) {
          LOG.error(e);
        }
      };
    }.start();
  }

  /**
   * This method will be called upon application start.
   */
  protected void onAppStart() {
  }
}
