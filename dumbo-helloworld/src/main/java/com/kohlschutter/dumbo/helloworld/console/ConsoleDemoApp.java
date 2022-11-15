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

import java.io.IOException;

import com.kohlschutter.dumbo.DumboSession;
import com.kohlschutter.dumbo.RPCRegistry;
import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.dumbo.bootstrap.BootstrapSupport;
import com.kohlschutter.dumbo.console.Console;
import com.kohlschutter.dumbo.console.ConsoleSupport;
import com.kohlschutter.dumbo.unix.TcpAndUnixAppHTTPServer;

/**
 * This demo shows how one can use the Console.
 */
public class ConsoleDemoApp extends ServerApp {
  public static void main(String[] args) throws IOException {
    final ConsoleDemoApp app = new ConsoleDemoApp();
    new TcpAndUnixAppHTTPServer(app, "consoleDemo.jsp", ConsoleDemoApp.class.getResource(
        "/com/kohlschutter/dumbo/helloworld/webapp/")).startAndWait();
  }

  @Override
  protected void initExtensions() {
    registerExtension(new BootstrapSupport());
    registerExtension(new ConsoleSupport());
  }

  /**
   * This is some state that is not shared across pages.
   */
  private static final class State {
    boolean enteredCommand = false;
    int n = -1;
  }

  @Override
  protected void initRPC(final RPCRegistry registry) {
    registry.registerRPCService(CommandLineService.class, new CommandLineService() {
      final String[] colors = new String[] {"red", "coral", "gold", "green", "blue", "fuchsia"};

      int count = 0; // this is an application-level state (i.e., shared across pages!)

      @Override
      public void sendLine(String line) {
        DumboSession session = DumboSession.getSession();
        session.getOrCreatePageAttribute(State.class, State::new).enteredCommand = true;
        final String color = colors[count++ % colors.length];

        session.getConsole().add(new ColorMessage(line, color));
      }
    });
  }

  @Override
  protected void onAppLoaded(DumboSession session) {
    Console console = session.getConsole();

    console.println("Please enter some text into the >> text box << above!");
    console.println("Java version: " + System.getProperty("java.version"));

    State state = session.getOrCreatePageAttribute(State.class, State::new);

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

          if (state.enteredCommand) {
            state.n = 0;
            state.enteredCommand = false;
          }
          switch (state.n) {
            case -1:
              console.println("Hello World from the server. Please enter some text!");
              state.n = 0;
              break;
            case 0:
              console.println("Thank you for entering some text");
              break;
            case 1:
              console.println("Please enter some text");
              break;
            case 2:
              console.println("Come on, please enter _anything_");
              break;
            case 3:
              console.println("Last chance!");
              break;
            case 4:
              console.add(new UserInputException("Too late!"));
              break;
            default:
              // silence
          }

          state.n++;
        }
      }
    }.start();
  }
}
