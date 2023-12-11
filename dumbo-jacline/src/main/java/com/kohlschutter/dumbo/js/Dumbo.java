/*
 * dumbo-jacline
 *
 * Copyright 2023 Christian Kohlschütter
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
package com.kohlschutter.dumbo.js;

import org.eclipse.jdt.annotation.NonNull;

import com.kohlschutter.jacline.annotations.JsImport;
import com.kohlschutter.jacline.lib.function.JsRunnable;

import elemental2.dom.Node;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Provide access to Dumbo from an app powered by
 * <a href="https://github.com/kohlschutter/jacline">jacline</a>.
 *
 * @author Christian Kohlschütter
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Dumbo")
@JsImport
public class Dumbo {
  public static native void keep(Object obj);

  public static native <T> @NonNull T getService(Class<T> clazz);

  public static native void setServiceAlias(String alias, String serviceName);

  @JsOverlay
  public static void setText(Node node, String selector, Object text) {
    setText0(node, selector, text);
  }

  @JsOverlay
  public static void setText(String selector, Object text) {
    setText0(selector, text);
  }

  @JsMethod(name = "setText")
  private static native void setText0(Object first, Object... args);

  public static native void whenLoaded(JsRunnable op);

  public static native void whenStatic(JsRunnable op);

  public static native void whenLive(JsRunnable op);

  public static native void whenReady(JsRunnable op);

  @JsOverlay
  public static Object cloneBySelector(String selector) {
    return clone(selector);
  }

  @JsOverlay
  public static Object cloneTemplateBySelectors(String templateSelector, String selector) {
    return cloneTemplate(templateSelector, selector);
  }

  private static native Object clone(Object arg);

  private static native Object cloneTemplate(Object template, Object arg);
}
