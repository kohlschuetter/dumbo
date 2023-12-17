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
package com.kohlschutter.dumbo;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboContent;
import com.kohlschutter.dumbo.api.DumboServer;
import com.kohlschutter.dumbo.api.DumboServerBuilder;

@SuppressWarnings("hiding")
public class DumboServerImplBuilder implements DumboServerBuilder {
  private boolean prewarm = false;
  private int port;
  private Class<? extends DumboApplication> application;
  private boolean webappSet = false;
  private URL webapp;
  private String prefix = "";
  private Path[] paths;

  public DumboServerImplBuilder() {
  }

  @Override
  public DumboServer build() throws IOException {
    ServerApp app = new ServerApp(application);
    if (!webappSet) {
      webapp = DumboServerImpl.getWebappBaseURL(app);
    }
    return new DumboServerImpl(prewarm, port, app, prefix, webapp, null, paths);
  }

  @Override
  public DumboServerBuilder withPort(int port) {
    this.port = port;
    return this;
  }

  @Override
  public DumboServerBuilder withApplication(Class<? extends DumboApplication> application) {
    this.application = Objects.requireNonNull(application);
    return this;
  }

  @Override
  public DumboServerBuilder withWebapp(URL resource) {
    this.webappSet = true;
    this.webapp = resource;
    return this;
  }

  @Override
  public DumboServerBuilder withPrefix(String prefix) {
    this.prefix = Objects.requireNonNull(prefix);
    return this;
  }

  @Override
  public DumboServerBuilder withContent(Path... paths) {
    this.paths = Arrays.copyOf(Objects.requireNonNull(paths), paths.length);
    return this;
  }

  @Override
  public DumboServerBuilder withContent(DumboContent content) {
    return withContent(content.toContentPaths());
  }

  @Override
  public DumboServerBuilder initFromEnvironmentVariables() {
    EnvHelper.checkEnv("DUMBO_CONTENT_SOURCE", (v) -> {
      DumboContent content;
      try {
        content = DumboContent.openExisting(Path.of(v));
      } catch (IOException e) {
        throw new IllegalArgumentException("Not a content directory: DUMBO_CONTENT_SOURCE=" + v, e);
      }
      withContent(content);
    });
    EnvHelper.checkEnv("PORT", (v) -> { // for Google AppEngine, etc.
      int port = Integer.parseInt(v);
      withPort(port);
    });
    EnvHelper.checkEnv("DUMBO_SERVER_PORT", (v) -> {
      int port = Integer.parseInt(v);
      withPort(port);
    });

    return this;
  }

  public DumboServerBuilder enablePrewarm() {
    this.prewarm = true;
    return this;
  }
}
