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

import java.io.IOException;
import java.net.URI;

/**
 * A Dumbo server is something that exposes a {@link DumboApplication} via HTTP, etc.
 *
 * @author Christian Kohlschütter
 */
public interface DumboServer {
  /**
   * Starts the server, unless it already is started.
   *
   * @return This instance itself.
   * @throws InterruptedException on interruption.
   */
  DumboServer start() throws IOException, InterruptedException;

  /**
   * Waits until the started server reaches a somewhat idle state.
   *
   * @return This instance itself.
   * @throws InterruptedException on interruption.
   */
  DumboServer awaitIdle() throws InterruptedException;

  /**
   * Returns a {@link URI} for this server; the URI may change upon {@link #start()}.
   *
   * @return The URI.
   */
  URI getURI();
}
