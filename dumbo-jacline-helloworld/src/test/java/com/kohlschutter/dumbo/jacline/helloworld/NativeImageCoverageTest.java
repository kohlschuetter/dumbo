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
import java.util.ServiceLoader;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.kohlschutter.dumbo.api.DumboServer;
import com.kohlschutter.dumbo.api.DumboServerProvider;

public class NativeImageCoverageTest {
  @Test
  public void testCoverage() throws Exception {
    assumeTrue(Locations.isAvailable());

    DumboServer server = ServiceLoader.load(DumboServerProvider.class).findFirst().get()
        .getDumboServer().start();

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
  }

  private static void load(URI uri) throws IOException {
    try (InputStream in = uri.toURL().openConnection().getInputStream()) {
      in.transferTo(OutputStream.nullOutputStream());
    }
  }
}
