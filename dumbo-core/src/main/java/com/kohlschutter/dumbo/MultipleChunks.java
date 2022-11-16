package com.kohlschutter.dumbo;

/**
 * A series of chunks, encapsulated into one.
 */
public final class MultipleChunks {
  private Object[] chunks;

  MultipleChunks(final Object[] chunks) {
    this.chunks = chunks;
  }

  /**
   * Returns the encapsulated chunks.
   *
   * @return The chunks.
   */
  public Object[] getChunks() {
    return chunks;
  }
}
