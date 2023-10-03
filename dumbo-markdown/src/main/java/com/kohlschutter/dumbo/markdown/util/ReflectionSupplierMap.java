/*
 * dumbo-markdown
 *
 * Copyright 2022,2023 Christian Kohlschütter
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
package com.kohlschutter.dumbo.markdown.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class ReflectionSupplierMap<V> extends SupplierMap<String, V> {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface ValueSupplier {
    String[] key();
  }

  @SuppressWarnings("unchecked")
  public ReflectionSupplierMap(Class<? extends V> valueType, String... partialOrder) {
    super();

    Map<String, Function<String, V>> ms = new LinkedHashMap<>();

    for (Method m : getClass().getMethods()) {
      if (m.isAnnotationPresent(ValueSupplier.class) && m.canAccess(this)) {
        @SuppressWarnings("null")
        ValueSupplier vs = m.getAnnotation(ValueSupplier.class);
        String[] keys = vs.key();
        if (keys.length == 0) {
          keys = new String[] {m.getName()};
        }

        if (!valueType.isAssignableFrom(m.getReturnType())) {
          throw new IllegalStateException("Bad return type for method: " + m);
        }

        switch (m.getParameterCount()) {
          case 0:
            for (String k : keys) {
              if (ms.containsKey(k)) {
                throw new IllegalStateException("Key already exists: " + k);
              }
              ms.put(k, (k2) -> {
                try {
                  return (V) m.invoke(this);
                } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e1) {
                  throw new IllegalStateException(e1);
                }
              });
            }
            break;
          case 1:
            Class<?> type = m.getParameterTypes()[0];
            if (!type.isAssignableFrom(String.class)) {
              throw new IllegalStateException("Bad parameter type for method: " + m);
            }
            for (String k : keys) {
              if (ms.containsKey(k)) {
                throw new IllegalStateException("Key already exists: " + k);
              }
              ms.put(k, (k2) -> {
                try {
                  return (V) m.invoke(this, k2);
                } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e1) {
                  throw new IllegalStateException(e1);
                }
              });
            }
            break;
          default:
            throw new IllegalStateException("Bad parameters for method: " + m);
        }
      }
    }

    for (String po : partialOrder) {
      Function<String, V> v = ms.get(po);
      ms.remove(po);
      if (v != null) {
        putSupplied(po, v);
      }
    }
    for (Map.Entry<String, Function<String, V>> en : ms.entrySet()) {
      putSupplied(en.getKey(), en.getValue());
    }
  }
}
