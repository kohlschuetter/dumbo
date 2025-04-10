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
   * @throws IOException on error.
   */
  protected abstract void onClosed(boolean success) throws IOException;
}
