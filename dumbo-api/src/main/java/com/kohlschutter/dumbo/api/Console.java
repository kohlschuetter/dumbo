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
package com.kohlschutter.dumbo.api;

import java.io.Closeable;
import java.io.PrintWriter;

public interface Console extends Closeable {
  /**
   * Adds some data to the output.
   *
   * @param o The object to be added to the output.
   */
  void add(Object o);

  /**
   * Convenience method for {@code getPrintWriter().println(s);}.
   *
   * @param s The string to print.
   */
  void println(String s);

  /**
   * Convenience method for {@code getPrintWriter().println(o);}.
   *
   * @param o The object to print.
   */
  void println(Object o);

  /**
   * Convenience method for {@code getPrintWriter().println();}.
   */
  void println();

  /**
   * Tells the app to clear the console.
   */
  void clear();

  /**
   * Returns a {@link PrintWriter} that allows the textual data to be sent as String objects.
   *
   * @return This console's {@link PrintWriter}.
   */
  PrintWriter getPrintWriter();

  /**
   * Requests the application to gracefully shutdown.
   */
  default void shutdown() {
    shutdown(true);
  }

  /**
   * Requests the application to gracefully shutdown.
   */
  void shutdown(boolean clean);
}
