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
