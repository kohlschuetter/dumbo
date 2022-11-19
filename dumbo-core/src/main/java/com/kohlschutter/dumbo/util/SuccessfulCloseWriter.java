package com.kohlschutter.dumbo.util;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * A {@link FilterWriter} that calls some operation upon {@link #close()}, with a {@code successful}
 * boolean that can be set separately.
 * 
 * @author Christian Kohlschütter
 */
public abstract class SuccessfulCloseWriter extends FilterWriter {
  private boolean successful = false;

  /**
   * Creates a new {@link SuccessfulCloseWriter}, wrapping the given {@link Writer}.
   * 
   * @param writer The writer to wrap.
   */
  public SuccessfulCloseWriter(Writer writer) {
    super(writer);
  }

  /**
   * Returns the value set by {@link #setSuccessful(boolean)}; {@code false} by default.
   * 
   * @return The state.
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Sets the "success state" (which is {@code false} by default).
   * 
   * @param successful The state.
   */
  public void setSuccessful(boolean successful) {
    this.successful = successful;
  }

  @Override
  public void close() throws IOException {
    super.close();
    onClosed(successful);
  }

  /**
   * Called as the last step in {@link #close()}.
   * 
   * @param success The state set via {@link #setSuccessful(boolean)}; {@code false} by default.
   * @throws IOException
   */
  protected abstract void onClosed(boolean success) throws IOException;
}
