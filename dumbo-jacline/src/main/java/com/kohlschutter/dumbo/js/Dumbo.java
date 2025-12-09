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

import com.kohlschutter.dumbo.annotations.DumboService;
import com.kohlschutter.jacline.annotations.JsImplementationProvidedSeparately;
import com.kohlschutter.jacline.annotations.JsImport;
import com.kohlschutter.jacline.lib.coding.CodingException;
import com.kohlschutter.jacline.lib.coding.CodingServiceProvider;
import com.kohlschutter.jacline.lib.coding.Decodables;
import com.kohlschutter.jacline.lib.coding.Decoder;
import com.kohlschutter.jacline.lib.coding.Dictionary;
import com.kohlschutter.jacline.lib.coding.KeyDecoder;
import com.kohlschutter.jacline.lib.function.JsFunctionCallback;
import com.kohlschutter.jacline.lib.function.JsRunnable;
import com.kohlschutter.jacline.lib.log.CommonLog;

import elemental2.dom.Node;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Provide access to Dumbo from an app powered by
 * <a href="https://github.com/kohlschutter/jacline">jacline</a>.
 *
 * @author Christian Kohlschütter
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL)
@JsImport
public class Dumbo {
  public static native void keep(Object obj);

  public static native <T> @NonNull T getService(Class<T> clazz);

  public static native void setServiceAlias(String alias, String serviceName);

  @SuppressWarnings("unused")
  @JsImplementationProvidedSeparately
  public static String resolveServiceTypeFromAlias(Class<?> clazz) {
    if (!clazz.isAnnotationPresent(DumboService.class)) {
      return null;
    }
    @SuppressWarnings("null")
    DumboService service = clazz.getDeclaredAnnotation(DumboService.class);
    if (service == null) {
      return null;
    }

    String rpcName = service.rpcName();
    if (rpcName.isEmpty()) {
      return clazz.getName();
    } else {
      return rpcName;
    }
  }

  @JsOverlay
  public static String registerCodable(Class<?> clazz, Decoder<?> dec) {
    String codedType = resolveServiceTypeFromAlias(clazz);
    Decodables.setDecoder(codedType, dec);
    return codedType;
  }

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

  @JsOverlay
  public static void setConsole(String targetElement) {
    setConsole(targetElement, null);
  }

  @JsOverlay
  public static void setConsole(String targetElement,
      JsFunctionCallback<Object, Object> objConverter) {
    setConsole0(targetElement, objConverter, null);
  }

  @JsOverlay
  public static void setConsole(String targetElement,
      Dictionary<JsFunctionCallback<? extends Object, Object>> javaClassMap,
      JsFunctionCallback<? extends Object, Object> fallbackObjConverter) {
    setConsole0(targetElement, fallbackObjConverter, javaClassMap);
  }

  @JsMethod(name = "setConsole")
  private static native void setConsole0(Object targetElement,
      JsFunctionCallback<? extends Object, Object> objConverter,
      Dictionary<JsFunctionCallback<? extends Object, Object>> javaClassMap);

  public static native Object consoleDefaultObjConverter(Object object);

  public static native void whenLoaded(JsRunnable op);

  public static native void whenStatic(JsRunnable op);

  public static native void whenLive(JsRunnable op);

  public static native void whenReady(JsRunnable op);

  @JsOverlay
  public static Node cloneBySelector(String selector) {
    return clone(selector);
  }

  @JsOverlay
  public static Node cloneNode(Node node) {
    return clone(node);
  }

  @JsOverlay
  public static Node cloneTemplateBySelectors(String templateSelector, String selector) {
    return cloneTemplate(templateSelector, selector);
  }

  private static native Node clone(Object arg);

  private static native Node cloneTemplate(Object template, Object arg);

  @SuppressWarnings("unchecked")
  @JsOverlay
  public static <T> void registerConsoleObject(
      Dictionary<JsFunctionCallback<? extends Object, Object>> javaClassMap, Class<T> klazz,
      JsFunctionCallback<T, Object> fc) {
    final CodingServiceProvider CSP = CodingServiceProvider.getDefault();
    String key = Dumbo.resolveServiceTypeFromAlias(klazz);
    if (key == null) {
      throw new IllegalArgumentException("Not registered: " + klazz);
    }
    javaClassMap.put(key, (jsObj) -> {
      try (KeyDecoder dec = CSP.keyDecoder(KeyDecoder.ANY_CODED_TYPE, jsObj)) {
        return fc.apply((T) Decodables.getDecoder(key).decode(dec));
      } catch (CodingException e) {
        CommonLog.error("Coding exception", e);
        return null;
      }
    });
  }

  static native void registerMarshallFilters(PreMarshalFunction preMarshal,
      PostUnmarshalFunction postUnmarshal, ErrorUnmarshalFunction errorUnmarshal);

  @FunctionalInterface
  @JsFunction
  interface PreMarshalFunction {
    Object preMarshallObject(Object obj);
  }

  @FunctionalInterface
  @JsFunction
  interface PostUnmarshalFunction {
    Object postUnmarshallObject(String javaClass, Object obj);
  }

  @FunctionalInterface
  @JsFunction
  interface ErrorUnmarshalFunction {
    Object unmarshallError(RpcError error);
  }

  @JsType
  interface RpcError {
    @JsProperty
    int getCode();

    @JsProperty
    String getMessage();

    @JsProperty
    String getData();
  }

  @JsOverlay
  static void registerMarshallFiltersForJacline() {
    Dumbo.registerMarshallFilters((o) -> JaclineInit.preMarshallObject(o), (javaType, u) -> {
      final CodingServiceProvider CSP = CodingServiceProvider.getDefault();

      if (javaType != null && Decodables.hasDecoder(javaType)) {
        try (KeyDecoder dec = CSP.keyDecoder(javaType, u)) {
          u = Decodables.getDecoder(javaType).decode(dec);
        } catch (CodingException e) {
          CommonLog.error("Could not decode object for Jacline", e);
          throw new IllegalStateException(e);
        }
      }
      return u;
    }, (e) -> new RpcException(e.getMessage(), e.getCode(), e.getData()));
  }
}
