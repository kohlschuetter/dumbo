/*
 * Copyright 2022,2023 Christian Kohlschütter
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
package com.kohlschutter.dumbo.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PathUtils {
  private PathUtils() {
    throw new IllegalStateException("No instances");
  }

  public static String getFilename(Path p) {
    Path filename = p.getFileName();
    if (filename == null) {
      throw new IllegalStateException();
    }
    return filename.toString();
  }

  public static Path resolveSiblingAppendingSuffix(Path path, String suffix) {
    return path.resolveSibling(getFilename(path) + suffix);
  }

  public static void createAncestorDirectories(Path generatedCssPath) throws IOException {
    Path parent = generatedCssPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
  }

  public static Path relativizeSibling(Path base, Path path) {
    Path parent = base.getParent();
    if (parent == null) {
      throw new IllegalStateException();
    }
    return parent.relativize(path);
  }
}
