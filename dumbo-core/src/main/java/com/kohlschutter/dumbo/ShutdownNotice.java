/*
 * Copyright 2022 Christian Kohlschütter
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

/**
 * The "shutdown notice" that is sent to the client.
 *
 * NOTE: Do not rename/move this class without changing app-console.js.
 */
public final class ShutdownNotice {
  public static final ShutdownNotice CLEAN = new ShutdownNotice(true);
  public static final ShutdownNotice NOT_CLEAN = new ShutdownNotice(false);

  private boolean clean;

  private ShutdownNotice(boolean clean) {
    this.clean = clean;
  }

  /**
   * If {@code true}, consider this shutdown "clean". If {@code false}, assume there was an error.
   *
   * @return The "clean" state.
   */
  public boolean isClean() {
    return clean;
  }
}
