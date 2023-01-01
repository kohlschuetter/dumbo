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
package com.kohlschutter.dumbo.util;

import java.util.Iterator;

/**
 * Constructs an {@link Iterable} from an {@link Iterator}.
 *
 * Note that this is guaranteed to work only when you're iterating over the iterable at most once.
 *
 * @param <T> The wrapped type.
 * @author Christian Kohlschütter
 */
public final class IteratorIterable<T> implements Iterable<T> {
  private final Iterator<T> iterator;

  private IteratorIterable(Iterator<T> iterator) {
    this.iterator = iterator;
  }

  @Override
  public Iterator<T> iterator() {
    return iterator;
  }

  @SuppressWarnings("unchecked")
  public static <T> Iterable<T> of(Iterator<T> it) {
    if (it instanceof Iterable) {
      return ((Iterable<T>) it);
    } else {
      return new IteratorIterable<>(it);
    }
  }
}
