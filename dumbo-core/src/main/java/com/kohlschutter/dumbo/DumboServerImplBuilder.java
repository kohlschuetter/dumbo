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
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboContent;
import com.kohlschutter.dumbo.api.DumboServer;
import com.kohlschutter.dumbo.api.DumboServerBuilder;

@SuppressWarnings("hiding")
public class DumboServerImplBuilder implements DumboServerBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(DumboServerImplBuilder.class);

  private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

  private boolean prewarm = false;
  private int port;
  private Class<? extends DumboApplication> application;
  private boolean webappSet = false;
  private URL webapp;
  private String prefix = "";
  private Path[] paths;

  private InetAddress bindAddress = LOOPBACK;

  public DumboServerImplBuilder() {
  }

  @Override
  public DumboServer build() throws IOException {
    ServerApp app = new ServerApp(application);
    if (!webappSet) {
      webapp = DumboServerImpl.getWebappBaseURL(app);
    }
    return new DumboServerImpl(prewarm, bindAddress, port, app, prefix, webapp, null, paths);
  }

  @Override
  public DumboServerBuilder withBindAddress(InetAddress addr) {
    this.bindAddress = addr;
    return this;
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
    if (!webappSet) {
      withWebapp(null);
    }
    return this;
  }

  @Override
  public DumboServerBuilder withContent(DumboContent content) {
    return withContent(content.toContentPaths());
  }

  @Override
  public DumboServerBuilder initFromEnvironmentVariables() {
    if (LOG.isInfoEnabled()) {
      for (Map.Entry<String, String> en : System.getenv().entrySet()) {
        LOG.info("env: {}: {}", en.getKey(), en.getValue());
      }
      for (Map.Entry<Object, Object> en : System.getProperties().entrySet()) {
        LOG.info("sysprop: {}: {}", en.getKey(), en.getValue());
      }
    }

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
    EnvHelper.checkEnv("DUMBO_SERVER_ADDR", (v) -> {
      try {
        InetAddress addr;
        if ("*".equals(v)) {
          addr = null;
        } else {
          addr = InetAddress.getByName(v);
        }
        withBindAddress(addr);
      } catch (UnknownHostException e) {
        throw new IllegalStateException(e);
      }
    });

    return this;
  }

  public DumboServerBuilder enablePrewarm() {
    this.prewarm = true;
    return this;
  }
}
