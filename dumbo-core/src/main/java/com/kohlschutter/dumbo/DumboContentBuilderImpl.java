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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboContent;
import com.kohlschutter.dumbo.api.DumboContentBuilder;
import com.kohlschutter.dumbo.api.DumboServerBuilder;

@SuppressWarnings("hiding")
public class DumboContentBuilderImpl implements DumboContentBuilder {
  private Class<? extends DumboApplication> application;
  private boolean webappSet = false;
  private URL webapp;
  private String prefix = "";
  private boolean sourceMaps = false;

  private Path outputPath;
  private String primaryHostname;
  private boolean createCNAMEFile;

  public DumboContentBuilderImpl() {
  }

  @Override
  public DumboContent build() throws IOException {
    DumboServerBuilder serverBuilder = new DumboServerImplBuilder();

    Objects.requireNonNull(outputPath, "outputPath");

    Path staticOutput = outputPath.resolve("static");
    Path dynamicOutput = outputPath.resolve("dynamic");

    Files.createDirectories(staticOutput);
    Files.createDirectories(dynamicOutput);

    if (webappSet) {
      serverBuilder = serverBuilder.withWebapp(webapp);
    }
    DumboServerImpl server = (DumboServerImpl) serverBuilder //
        .withApplication(prefix, application) //
        .withPort(-1) // no need to bind on TCP
        .build();
    try {
      server.generateFiles(staticOutput, dynamicOutput, sourceMaps);
    } catch (InterruptedException e) {
      throw new IOException(e);
    }

    if (createCNAMEFile) {
      if (primaryHostname == null || primaryHostname.isEmpty()) {
        throw new IllegalArgumentException("Primary hostname not set (required for CNAME file)");
      }

      try (Writer out = Files.newBufferedWriter(staticOutput.resolve("CNAME"))) {
        out.write(primaryHostname.trim() + "\n");
      }
    }

    return new DumboContentImpl(outputPath);
  }

  @Override
  public DumboContentBuilder withApplication(Class<? extends DumboApplication> application) {
    this.application = Objects.requireNonNull(application);
    return this;
  }

  @Override
  public DumboContentBuilder withWebapp(URL resource) {
    this.webappSet = true;
    this.webapp = resource;
    return this;
  }

  @Override
  public DumboContentBuilder withPrefix(String prefix) {
    this.prefix = Objects.requireNonNull(prefix);
    return this;
  }

  @Override
  public DumboContentBuilder withOutputPath(Path outputPath) {
    this.outputPath = outputPath;
    return this;
  }

  @Override
  public DumboContent openExisting(Path outputPath) throws FileNotFoundException {
    if (!Files.isDirectory(outputPath)) {
      throw new FileNotFoundException("Not found: " + outputPath);
    }
    return new DumboContentImpl(outputPath);
  }

  @Override
  public DumboContentBuilder withSourceMaps(boolean sourceMaps) {
    this.sourceMaps = sourceMaps;
    return this;
  }

  @Override
  public DumboContentBuilder withPrimaryHostname(String hostname) {
    this.primaryHostname = hostname;
    return this;
  }

  @Override
  public DumboContentBuilder withCreateCNAMEFile(boolean cnameFile) {
    this.createCNAMEFile = cnameFile;
    return this;
  }
}
