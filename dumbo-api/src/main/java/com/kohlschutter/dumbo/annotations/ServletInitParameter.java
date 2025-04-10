/*
 * Copyright 2022-2025 Christian Kohlschütter
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
package com.kohlschutter.dumbo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServletInitParameter {
  String key();

  String value();

  /**
   * Specify how the value is provided.
   * <p>
   * If it's {@code String.class} (the default), the value specified as {@link #value()} is taken
   * as-is.
   * <p>
   * If it's another class, then a static method with the name specified in {@link #value()} is
   * called to obtain the actual value. If the type is of a known scoped-singleton instance, e.g.,
   * {@code ServerApp}, the method name may also refer to an instance method.
   * <p>
   * By contract, the method must be idempotent. Moreover, there is no guarantee as to when the
   * method is called.
   *
   * @return The provider class, or {@code String.class} (the default) for "string as-is".
   */
  Class<?> valueProvider() default String.class;
}
