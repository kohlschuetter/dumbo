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

/**
 * Specifies a {@code WASM} resource for use with an extension.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebAssemblyResource {
  /**
   * The relative path to the resource, e.g., {@code wasm/index.wasm}.
   *
   * @return The relative path.
   */
  String[] value();

  /**
   * An optional group the WASM resource may be integrated with.
   *
   * As a general rule, only resources with the same license should be grouped.
   *
   * @return The group, or empty if no grouping desired.
   */
  String group() default "";

  /**
   * An optional resource that may be excluded if certain optimization conditions are met.
   *
   * @return {@code true} if optional.
   */
  boolean optional() default false;

}
