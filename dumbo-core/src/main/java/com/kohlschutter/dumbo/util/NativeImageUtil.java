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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;

public final class NativeImageUtil {
  private static final Logger LOG = LoggerFactory.getLogger(NativeImageUtil.class);

  private NativeImageUtil() {
  }

  public static URL walkResources(String rootPath, Function<String, URL> resourceResolver) {
    if (!rootPath.endsWith("/")) {
      rootPath += "/";
    }
    final String prefix = rootPath;

    URL u = resourceResolver.apply(rootPath);
    if (u != null) {
      Path root = urlToPath(u);
      try {
        LOG.debug("Walking resource tree for root {}", root);
        Files.walk(root).forEach((p) -> {
          URL url = resourceResolver.apply(prefix + root.relativize(p).toString());
          LOG.debug("Found {}", url);
        });
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    return u;
  }

  @SuppressWarnings("PMD.PreserveStackTrace")
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  private static Path urlToPath(URL url) {
    URI uri;
    try {
      uri = url.toURI();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
    try {
      return Paths.get(uri);
    } catch (FileSystemNotFoundException e) {
      try {
        FileSystems.newFileSystem(uri, Collections.emptyMap());
      } catch (IOException e1) {
        IllegalStateException ex = new IllegalStateException(e);
        ex.addSuppressed(e1);
        throw ex;
      }
      return Paths.get(uri);
    }
  }
}
