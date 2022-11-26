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
package com.kohlschutter.dumbo.helloworld.console;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;
import com.kohlschutter.dumbo.api.DumboSession;

public class CommandLineServiceImpl implements CommandLineService {
  final String[] colors = new String[] {"red", "coral", "gold", "green", "blue", "fuchsia"};

  int count = 0; // this is an application-level state (i.e., shared across pages!)

  @Override
  @SuppressFBWarnings("NN_NAKED_NOTIFY")
  public void sendLine(String line) {
    DumboSession session = DumboSession.getSession();
    session.getOrCreatePageAttribute(State.class, State::new).enteredCommand = true;
    final String color = colors[count++ % colors.length];

    session.getConsole().add(new ColorMessage(line, color));
    synchronized (session) {
      session.notifyAll();
    }
  }
}
