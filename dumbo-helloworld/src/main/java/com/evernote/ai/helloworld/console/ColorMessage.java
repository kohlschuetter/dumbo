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
package com.evernote.ai.helloworld.console;

import com.evernote.ai.dumbo.console.Console;

/**
 * A simple message that we send via the {@link Console}.
 */
public final class ColorMessage {
  final String message;
  final String color;

  ColorMessage(final String message, final String color) {
    this.message = message;
    this.color = color;
  }

  public String getMessage() {
    return message;
  }

  public String getColor() {
    return color;
  }
}
