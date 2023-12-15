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

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.kohlschutter.dumbo.AppHTTPServer;
import com.kohlschutter.dumbo.ServerApp;

public class NativeImageCoverageTest {
  @Test
  public void testCoverage() throws Exception {
    assumeTrue(Locations.isAvailable());

    Path staticPath = Locations.getStaticOut();
    Path dynamicPath = Locations.getDynamicOut();

    AppHTTPServer server = new AppHTTPServer(0, new ServerApp(HelloWorldApp.class), "" //
        , staticPath, dynamicPath //
    ).start();

    URI rootUri = server.getURI();
    try {
      load(rootUri);
      try {
        load(rootUri.resolve(UUID.randomUUID().toString())); // trigger error page
      } catch (FileNotFoundException ignore) {
        // ignore (expected);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    server.shutdown();
  }

  private static void load(URI uri) throws IOException {
    try (InputStream in = uri.toURL().openConnection().getInputStream()) {
      in.transferTo(OutputStream.nullOutputStream());
    }
  }
}
