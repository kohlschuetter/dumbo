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
public class ConsoleDemoApp extends ServerApp {
  public static void main(String[] args) throws IOException {
    final ConsoleDemoApp app = new ConsoleDemoApp();
    new AppHTTPServer(app, "consoleDemo.jsp", ConsoleDemoApp.class
        .getResource("/com/evernote/ai/helloworld/webapp/")).startAndWait();
  }

  @Override
  protected void initExtensions() {
    registerExtension(new BootstrapSupport());
    registerExtension(new ConsoleSupport());
  }

  private int n = -1;
  private Console console;
  private boolean enteredCommand = false;

  @Override
  protected void initRPC(final RPCRegistry registry) {
    registry.registerRPCService(CommandLineService.class, new CommandLineService() {
      final String[] colors = new String[] {
          "red", "coral", "gold", "green", "blue", "fuchsia"};

      int count = 0;

      @Override
      public void sendLine(String line) {
        enteredCommand = true;
        final String color = colors[count++ % colors.length];
        console.add(new ColorMessage(line, color));
      }
    });

    console = registerCloseable(new Console(this, registry));
  }

  @Override
  protected void onStart() {
    final PrintWriter consoleOut = console.getPrintWriter();

    consoleOut.println("Please enter some text into the >> text box << above!");
    consoleOut.println("Java version: " + System.getProperty("java.version"));

    new Thread() {
      {
        setDaemon(true);
      }

      @Override
      public void run() {
        while (!Thread.interrupted()) {
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
          }
          if (enteredCommand) {
            n = 0;
            enteredCommand = false;
          }
          switch (n) {
            case -1:
              consoleOut.println("Hello World from the server. Please enter some text!");
              n = 0;
              break;
            case 0:
              consoleOut.println("Thank you for entering some text");
              break;
            case 1:
              consoleOut.println("Please enter some text");
              break;
            case 2:
              consoleOut.println("Come on, please enter _anything_");
              break;
            case 3:
              consoleOut.println("Last chance!");
              break;
            case 4:
              console.add(new UserInputException("Too late!"));
              break;
            default:
              // silence
          }

          n++;
        }
      }
    }.start();
  }

  @Override
  protected void onAppReloaded(final String pageId) {
    console.add(new PageReloadException("Page was reloaded"));
    n = -1;
  }
}
