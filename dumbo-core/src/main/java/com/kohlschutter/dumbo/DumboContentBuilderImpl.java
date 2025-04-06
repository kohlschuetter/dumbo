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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboContent;
import com.kohlschutter.dumbo.api.DumboContentBuilder;
import com.kohlschutter.dumbo.api.DumboServerBuilder;

@SuppressWarnings("hiding")
public class DumboContentBuilderImpl implements DumboContentBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(DumboContentBuilderImpl.class);

  private Class<? extends DumboApplication> application;
  private boolean webappSet = false;
  private URL webapp;
  private String prefix = "";
  private boolean sourceMaps = false;

  private Path outputPath;
  private String primaryHostname;
  private Set<String> visitRelativeUrls = new HashSet<>();
  private boolean createCNAMEFile;
  private Path jspSourceOutputPath;
  private Path jspClassOutputPath;

  public DumboContentBuilderImpl() {
  }

  private void copyJspFiles(DumboServerImpl server, Path outputPath, String fileSuffix)
      throws IOException {
    if (outputPath == null) {
      return;
    }
    File jspWorkDir = server.getMainApplication().getJspWorkDir();

    Path jspWorkDirPath = jspWorkDir.toPath();

    List<Path> list = Files.walk(jspWorkDirPath).filter((p) -> Files.isDirectory(p) || p
        .getFileName().toString().endsWith(fileSuffix)).toList();

    LOG.info("Compiled JSP {} files to copy to {}", fileSuffix, jspWorkDirPath);
    for (Path p : list) {
      Path relPath = jspWorkDirPath.relativize(p);

      Path targetPath = outputPath.resolve(relPath);
      if (Files.isDirectory(p)) {
        Files.createDirectories(targetPath);
      } else {
        LOG.info("Copying JSP {} file: {}", fileSuffix, relPath);
        Files.copy(p, targetPath, StandardCopyOption.REPLACE_EXISTING);
      }
    }

  }

  @Override
  public DumboContent build() throws IOException {
    DumboServerBuilder serverBuilder = new DumboServerImplBuilder();

    if (webappSet) {
      serverBuilder = serverBuilder.withWebapp(webapp);
    }
    DumboServerImpl server = (DumboServerImpl) serverBuilder //
        .withApplication(prefix, application) //
        .withPort(-1) // no need to bind on TCP
        .withPrewarm(true) //
        .withPrewarmRelativeURL(visitRelativeUrls.toArray(new String[0])) //
        .build();

    if (outputPath == null) {
      try {
        server.startAwaitAndStopIfNotYetStarted();
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    }

    copyJspFiles(server, jspSourceOutputPath, ".java");
    copyJspFiles(server, jspClassOutputPath, ".class");

    if (outputPath == null) {
      return null;
    }

    Path staticOutput = outputPath.resolve("static");
    Path dynamicOutput = outputPath.resolve("dynamic");

    Files.createDirectories(staticOutput);
    Files.createDirectories(dynamicOutput);

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

  @Override
  public DumboContentBuilder withVisitRelativeURL(String... relativeURL) {
    this.visitRelativeUrls.addAll(Arrays.asList(relativeURL));
    return this;
  }

  @Override
  public DumboContentBuilder withJspSourceOutputPath(Path outputPath) {
    this.jspSourceOutputPath = outputPath;
    return this;
  }

  @Override
  public DumboContentBuilder withJspClassOutputPath(Path outputPath) {
    this.jspClassOutputPath = outputPath;
    return this;
  }
}
