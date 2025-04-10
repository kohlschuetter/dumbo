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
package com.kohlschutter.dumbo.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboContent;
import com.kohlschutter.dumbo.api.DumboContentBuilder;

public final class GenerateDumboContent {
  private static final Logger LOG = LoggerFactory.getLogger(GenerateDumboContent.class);

  private GenerateDumboContent() {

  }

  public static void main(String[] args) throws ClassNotFoundException, IOException {
    if (args.length != 2) {
      System.err.println("args required: <application class> <content output directory>");
      System.exit(1);
      return;
    }

    Class<?> klazz = Class.forName(args[0]);
    if (!DumboApplication.class.isAssignableFrom(klazz)) {
      throw new IllegalStateException("Not a DumboApplication: " + klazz);
    }
    @SuppressWarnings("unchecked")
    Class<? extends DumboApplication> applicationClass = (Class<? extends DumboApplication>) klazz;

    Path outputPath = Path.of(args[1]);
    if (Files.exists(outputPath)) {
      if (Files.isDirectory(outputPath) && Files.isDirectory(outputPath.resolve("static")) && Files
          .isDirectory(outputPath.resolve("dynamic"))) {
        // existing output directory
      } else if (Files.list(outputPath).count() > 0) {
        LOG.warn("Output directory exists and is not empty: {}", outputPath);
      }
    }

    DumboContent content = DumboContentBuilder.begin() //
        .withApplication(applicationClass) //
        .withOutputPath(outputPath) //
        .build();

    System.out.println("Output created: " + content.getBasePath());
  }
}
