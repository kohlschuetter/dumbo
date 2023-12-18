/*
 * dumbo-jacline-helloworld
 *
 * Copyright 2023 Christian Kohlschütter
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
package com.kohlschutter.dumbo.jacline.helloworld;

import java.io.IOException;

import com.kohlschutter.dumbo.DumboServerImplBuilder;
import com.kohlschutter.dumbo.api.DumboContent;
import com.kohlschutter.dumbo.api.DumboServer;
import com.kohlschutter.dumbo.api.DumboServerBuilder;
import com.kohlschutter.dumbo.util.DevTools;

public class OpenStatic {
  public static void main(String[] args) throws IOException, InterruptedException {
    DumboServer server = ((DumboServerImplBuilder) DumboServerBuilder.begin()) //
        .initFromEnvironmentVariables() //
        .withApplication(HelloWorldApp.class) //
        .withContent(DumboContent.openExisting(Locations.getOutputPath())) //
        .build().start();
    DevTools.openURL(server);
  }
}
