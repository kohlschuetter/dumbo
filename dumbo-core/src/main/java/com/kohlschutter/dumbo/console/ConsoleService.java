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
package com.kohlschutter.dumbo.console;

import com.kohlschutter.dumbo.annotations.DumboService;
import com.kohlschutter.dumbo.api.Console;

/**
 * Provides access to an object-oriented console ("System.out")
 *
 * @see Console
 */
@DumboService(rpcName = "ConsoleService")
public interface ConsoleService {
  /**
   * Requests the next chunk of input readable from the console.
   *
   * @return The next chunk, or the empty string if no chunk was readable, or {@code null} if the
   *         console has been closed.
   */
  Object requestNextChunk();
}
