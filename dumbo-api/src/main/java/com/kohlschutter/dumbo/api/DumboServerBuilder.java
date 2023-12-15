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
package com.kohlschutter.dumbo.api;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Builds a {@link DumboServer} instance.
 *
 * @author Christian Kohlschütter
 */
public interface DumboServerBuilder {
  DumboServer build() throws IOException;

  DumboServerBuilder withPort(int port);

  DumboServerBuilder withPrefix(String prefix);

  DumboServerBuilder withApplication(Class<? extends DumboApplication> application);

  DumboServerBuilder withWebapp(URL resource);

  DumboServerBuilder withContent(Path... paths);

  /**
   * Returns a new {@link DumboServerBuilder}.
   *
   * @return A new builder instance.
   */
  static DumboServerBuilder begin() {
    Optional<@NonNull DumboServerBuilder> first = ServiceLoader.load(DumboServerBuilder.class)
        .findFirst();
    if (first.isPresent()) {
      return Objects.requireNonNull(first.get());
    } else {
      throw new IllegalStateException("No builder available");
    }
  }
}
