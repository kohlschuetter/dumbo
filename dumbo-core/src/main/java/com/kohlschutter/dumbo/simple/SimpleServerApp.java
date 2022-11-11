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
package com.kohlschutter.dumbo.simple;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.kohlschutter.dumbo.Extension;
import com.kohlschutter.dumbo.RPCRegistry;
import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.dumbo.console.Console;
import com.kohlschutter.dumbo.console.ConsoleSupport;
import com.kohlschutter.dumbo.ext.AppDefaultsSupport;

/**
 * A simplified {@link ServerApp}, which allows extensions and services to be specified using Java
 * annotations.
 */
@Extensions({ConsoleSupport.class, AppDefaultsSupport.class})
public abstract class SimpleServerApp extends ServerApp {
  private static final Logger LOG = Logger.getLogger(SimpleServerApp.class);

  protected Console console;
  private final List<Class<Extension>> annotatedExtensions;
  private final List<Class<Object>> annotatedServices;

  protected SimpleServerApp() {
    super();

    this.annotatedExtensions = getClassesFromAnnotation(Extensions.class, Extension.class);
    this.annotatedServices = getClassesFromAnnotation(Services.class, Object.class);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void initRPC(final RPCRegistry registry) {
    this.console = new Console(this, registry);

    for (Class<Object> extClass : annotatedServices) {
      Class<?>[] interfaces = extClass.getInterfaces();
      registry.registerRPCService((Class<Object>) interfaces[0], newInstance(extClass));
    }
  }

  @Override
  protected void initExtensions() {
    for (Class<Extension> extClass : annotatedExtensions) {
      registerExtension(newInstance(extClass));
    }
  }

  private <T> T newInstance(Class<T> clazz) {
    try {
      try {
        return clazz.getConstructor(ServerApp.class).newInstance(this);
      } catch (NoSuchMethodException e) {
      }
      try {
        clazz.getConstructor();
        return clazz.newInstance();
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

      if (clazz == SimpleServerApp.class) {
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
          console.add(e);
        }
      };
    }.start();
  }

  /**
   * This method will be called upon application start.
   * 
   * @param consoleOut Will log to the browser, either to its console or a dedicated element.
   */
  protected void onAppStart() {
  }

  /**
   * Returns the {@link Console} for this app.
   * 
   * @return The console.
   */
  public Console getConsole() {
    return console;
  }
}
