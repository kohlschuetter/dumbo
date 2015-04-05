/**
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
package com.evernote.ai.dumbo;

import java.io.IOException;

/**
 * Thrown during extension dependency resolution to signal that a non-recoverable problem
 * occurred, e.g., a dependency conflict.
 */
public class ExtensionDependencyException extends IOException {
  /**
   * {@inheritDoc}
   */
  public ExtensionDependencyException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * {@inheritDoc}
   */
  public ExtensionDependencyException(String message) {
    super(message);
  }

  /**
   * {@inheritDoc}
   */
  public ExtensionDependencyException(Throwable cause) {
    super(cause);
  }
}
