/*
 * Copyright 2022-2025 Christian Kohlschütter
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
package com.kohlschutter.dumbo.helloworld.consoleout;

import java.io.IOException;
import java.io.PrintWriter;

import com.kohlschutter.dumbo.ConsoleSupport;
import com.kohlschutter.dumbo.annotations.EventHandlers;
import com.kohlschutter.dumbo.annotations.Services;
import com.kohlschutter.dumbo.api.Console;
import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboServer;
import com.kohlschutter.dumbo.api.DumboServerBuilder;
import com.kohlschutter.dumbo.api.DumboSession;
import com.kohlschutter.dumbo.api.EventHandler;
import com.kohlschutter.dumbo.bootstrap.BootstrapSupport;
import com.kohlschutter.dumbo.helloworld.DemoServiceImpl;
import com.kohlschutter.dumbo.util.DevTools;

/**
 * This demo shows how one can use the Console.
 */
@Services({DemoServiceImpl.class})
@EventHandlers({ConsoleOutDemoApp.class})
public class ConsoleOutDemoApp implements DumboApplication, BootstrapSupport, ConsoleSupport,
    EventHandler {
  public static void main(String[] args) throws IOException, InterruptedException {
    DumboServer server = DumboServerBuilder.begin() //
        .withMainApplication(ConsoleOutDemoApp.class) //
        .withWebapp(ConsoleOutDemoApp.class.getResource(
            "/com/kohlschutter/dumbo/helloworld/webapp/")).build().start();
    DevTools.openURL(server, "/consoleOutDemo.jsp");
  }

  @Override
  public void onAppLoaded(DumboSession session) {
    Console console = session.getConsole();
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
