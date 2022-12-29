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
