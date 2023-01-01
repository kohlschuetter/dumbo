/*
 * Copyright 2022,2023 Christian Kohlschütter
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

import java.io.IOException;

/**
 * A key to identify a certain implementation.
 *
 * @param <K> The type of the implementation.
 * @author Christian Kohlschütter
 * @see ServerApp#getImplementationByIdentity(ImplementationIdentity, Supplier)
 */
public final class ImplementationIdentity<K> {
  /**
   * Supplies implementations.
   *
   * @param <T> The type of the implementation.
   * @author Christian Kohlschütter
   */
  @FunctionalInterface
  public interface Supplier<T> {
    /**
     * Supplies an implementation.
     *
     * @return an implementation.
     * @throws IOException on error.
     */
    T get() throws IOException;
  }

  public ImplementationIdentity() {
  }
}
