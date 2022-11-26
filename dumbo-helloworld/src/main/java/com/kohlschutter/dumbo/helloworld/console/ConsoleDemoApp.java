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
import java.util.concurrent.CompletableFuture;

import com.kohlschutter.dumbo.AppHTTPServer;
import com.kohlschutter.dumbo.ConsoleSupport;
import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.dumbo.annotations.EventHandlers;
import com.kohlschutter.dumbo.annotations.Services;
import com.kohlschutter.dumbo.api.Application;
import com.kohlschutter.dumbo.api.Console;
import com.kohlschutter.dumbo.api.DumboSession;
import com.kohlschutter.dumbo.api.EventHandler;
import com.kohlschutter.dumbo.bootstrap.BootstrapSupport;
import com.kohlschutter.dumbo.util.DevTools;

/**
 * This demo shows how one can use the Console.
 */
@Services(CommandLineServiceImpl.class)
@EventHandlers(ConsoleDemoApp.class)
public class ConsoleDemoApp implements Application, BootstrapSupport, ConsoleSupport, EventHandler {
  public static void main(String[] args) throws IOException {
    new AppHTTPServer(new ServerApp(ConsoleDemoApp.class), "/", ConsoleDemoApp.class.getResource(
        "/com/kohlschutter/dumbo/helloworld/webapp/")) {

      @Override
      protected void onServerStart() {
        DevTools.openURL(this, "/consoleDemo.jsp");
      }

    }.startAndWait();
  }

  @Override
  public void onAppLoaded(DumboSession session) {
    Console console = session.getConsole();
    State state = session.getOrCreatePageAttribute(State.class, State::new);

    console.println("Please enter some text into the >> text box << above!");
    console.println("Java version: " + System.getProperty("java.version"));

    CompletableFuture.runAsync(() -> {
      while (!Thread.interrupted()) {
        synchronized (session) {
          try {
            session.wait(5000);
          } catch (InterruptedException e) {
          }
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
    });
  }
}
