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
package com.kohlschutter.dumbo.helloworld;

import java.io.IOException;

import com.kohlschutter.dumbo.AppHTTPServer;
import com.kohlschutter.dumbo.Extensions;
import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.dumbo.Services;
import com.kohlschutter.dumbo.ext.AppDefaultsSupport;
import com.kohlschutter.dumbo.util.DevTools;

/**
 * A simple "Hello world" demo app.
 */
@Extensions({AppDefaultsSupport.class})
@Services({DemoServiceImpl.class})
public class HelloWorldApp extends ServerApp {
  public static void main(String[] args) throws IOException {
    new AppHTTPServer(new HelloWorldApp()) {

      @Override
      protected void onServerStart() {
        DevTools.openURL(this, "/");
      }

    }.start();
  }
}
