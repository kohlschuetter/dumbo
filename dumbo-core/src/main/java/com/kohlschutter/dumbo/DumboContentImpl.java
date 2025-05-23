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
package com.kohlschutter.dumbo;

import java.nio.file.Path;

import com.kohlschutter.dumbo.api.DumboContent;

final class DumboContentImpl implements DumboContent {
  private final Path basePath;
  private final Path staticPath;
  private final Path dynamicPath;

  DumboContentImpl(Path basePath) {
    this.basePath = basePath;

    this.staticPath = basePath.resolve("static");
    this.dynamicPath = basePath.resolve("dynamic");
  }

  @Override
  public Path getBasePath() {
    return basePath;
  }

  @Override
  public Path[] toContentPaths() {
    return new Path[] {getStaticPath(), getDynamicPath()};
  }

  @Override
  public Path getStaticPath() {
    return staticPath;
  }

  @Override
  public Path getDynamicPath() {
    return dynamicPath;
  }
}
