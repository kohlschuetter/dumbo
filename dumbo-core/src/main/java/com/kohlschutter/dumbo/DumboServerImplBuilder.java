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
import java.util.Objects;

import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboServer;
import com.kohlschutter.dumbo.api.DumboServerBuilder;

@SuppressWarnings("hiding")
public class DumboServerImplBuilder implements DumboServerBuilder {
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
    return new DumboServerImpl(port, app, prefix, webapp, null, paths
    // , Locations.getStaticOut(), Locations.getDynamicOut()
    );
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
    this.paths = Objects.requireNonNull(paths);
    return null;
  }
}
