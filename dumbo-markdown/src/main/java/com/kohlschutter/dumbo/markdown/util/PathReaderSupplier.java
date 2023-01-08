/*
 * dumbo-markdown
 *
 * Copyright 2022,2023 Christian Kohlschütter
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
package com.kohlschutter.dumbo.markdown.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import com.kohlschutter.stringhold.IOSupplier;

public class PathReaderSupplier implements IOSupplier<Reader> {
  private final IOSupplier<Reader> supplier;
  private final String relativePath;
  private final String type;

  private PathReaderSupplier(IOSupplier<Reader> supplier, String relativePath, String type) {
    this.supplier = supplier;
    this.relativePath = relativePath;
    this.type = type;
  }

  public static PathReaderSupplier withContentsOf(String relativePath, File f, Charset cs) {
    return withContentsOf(null, relativePath, f, cs);
  }

  public static PathReaderSupplier withContentsOf(String relativePath, Path p, Charset cs) {
    return withContentsOf(null, relativePath, p, cs);
  }

  public static PathReaderSupplier withContentsOf(String relativePath, URL url, Charset cs) {
    return withContentsOf(null, relativePath, url, cs);
  }

  public static PathReaderSupplier withContentsOf(String type, String relativePath, File f,
      Charset cs) {
    return new PathReaderSupplier(IOReaderSupplier.withContentsOf(f, StandardCharsets.UTF_8),
        relativePath, type);
  }

  public static PathReaderSupplier withContentsOf(String type, String relativePath, Path p,
      Charset cs) {
    return new PathReaderSupplier(IOReaderSupplier.withContentsOf(p, StandardCharsets.UTF_8),
        relativePath, type);
  }

  public static PathReaderSupplier withContentsOf(String type, String relativePath, URL u,
      Charset cs) {
    return new PathReaderSupplier(IOReaderSupplier.withContentsOf(u, StandardCharsets.UTF_8),
        relativePath, type);
  }

  @Override
  public Reader get() throws IOException {
    return supplier.get();
  }

  public String getRelativePath() {
    return relativePath;
  }

  public String getType() {
    return type;
  }
}
