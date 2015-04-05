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
package com.evernote.ai.helloworld.consoleout;

import java.io.IOException;
import java.io.PrintWriter;

import com.evernote.ai.dumbo.AppHTTPServer;
import com.evernote.ai.dumbo.RPCRegistry;
import com.evernote.ai.dumbo.ServerApp;
import com.evernote.ai.dumbo.bootstrap.BootstrapSupport;
import com.evernote.ai.dumbo.console.Console;
import com.evernote.ai.dumbo.console.ConsoleSupport;

/**
 * This demo shows how one can use the Console.
 */
public class ConsoleOutDemoApp extends ServerApp {
  public static void main(String[] args) throws IOException {
    final ConsoleOutDemoApp app = new ConsoleOutDemoApp();
    new AppHTTPServer(app, "consoleOutDemo.jsp", ConsoleOutDemoApp.class
        .getResource("/com/evernote/ai/helloworld/webapp/")).startAndWait();
  }

  @Override
  protected void initExtensions() {
    registerExtension(new BootstrapSupport());
    registerExtension(new ConsoleSupport());
  }

  private Console console;

  @Override
  protected void initRPC(final RPCRegistry registry) {
    console = registerCloseable(new Console(this, registry));
  }

  @Override
  protected void onStart() {
    PrintWriter pw = console.getPrintWriter();
    pw.println("Printing a couple of lines");
    for (int i = 0; i < 10000; i++) {
      console.add("Line " + i);
    }
    pw.println("One more!");
    pw.println("What follows is the\nlast line");
    console.shutdown();
  }
}
