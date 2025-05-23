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
package com.kohlschutter.dumbo.helloworld;

import java.io.IOException;

import com.kohlschutter.dumbo.annotations.Services;
import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboServer;
import com.kohlschutter.dumbo.api.DumboServerBuilder;
import com.kohlschutter.dumbo.appdefaults.AppDefaultsSupport;
import com.kohlschutter.dumbo.util.DevTools;

/**
 * A simple "Hello world" demo.
 */
@Services({DemoServiceImpl.class})
public class HelloWorldApp implements DumboApplication, AppDefaultsSupport {
  public static void main(String[] args) throws IOException, InterruptedException {
    DumboServer server = DumboServerBuilder.begin() //
        .withMainApplication(HelloWorldApp.class) //
        .build().start();
    DevTools.openURL(server);
  }
}
