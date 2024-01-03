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
package com.kohlschutter.dumbo.markdown;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.efesnitch.PathWatcher;
import com.kohlschutter.util.PathUtil;
import com.sass_lang.embedded_protocol.InboundMessage.ImportResponse.ImportSuccess;
import com.sass_lang.embedded_protocol.OutputStyle;

import de.larsgrefer.sass.embedded.CompileSuccess;
import de.larsgrefer.sass.embedded.SassCompilationFailedException;
import de.larsgrefer.sass.embedded.SassCompiler;
import de.larsgrefer.sass.embedded.SassCompilerFactory;
import de.larsgrefer.sass.embedded.importer.CustomImporter;

/**
 * Compiles .scss to .css, supporting Liquid templates in the initial .scss file.
 *
 * @author Christian Kohlschütter
 */
final class ScssCompiler implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(ScssCompiler.class);
  private static final String CUSTOM_SASS_URI_SCHEME = "dumbo-sass-resource:";
  private final MarkdownSupportImpl mdSupport;
  private final ServerApp app;

  ScssCompiler(ServerApp app) throws IOException {
    this.app = app;
    mdSupport = app.getImplementationByIdentity(MarkdownSupportImpl.COMPONENT_IDENTITY,
        () -> new MarkdownSupportImpl(app));

    PathWatcher pathWatcher = app.getPathWatcher();
    Path sassPath = PathUtil.toPathIfPossible(app.getResource("markdown/_sass"));
    if (pathWatcher.mayRegister(sassPath)) {
      LOG.info("Watching for changes: {}", sassPath);
      pathWatcher.register(sassPath, (p) -> CssFilter.markForceReloadNextTime());
    }
  }

  private final class SassImporter extends CustomImporter {

    @Override
    public CustomImporter autoCanonicalize() {
      return this;
    }

    private URL getSassURL(String path) {
      return app.getResource("markdown/_sass/" + path);
    }

    @Override
    public ImportSuccess handleImport(String url) throws Exception {
      if (!url.startsWith(CUSTOM_SASS_URI_SCHEME)) {
        LOG.warn("Unsupported URL: {}", url);
        return null;
      }
      url = url.substring(CUSTOM_SASS_URI_SCHEME.length()) + ".scss";

      LOG.debug("Importing {}", url);

      URL resUrl = getSassURL(url);
      if (resUrl == null) {
        int slash = 0;
        while ((slash = url.indexOf("/", slash + 1)) != -1) {
          String prefix = url.substring(0, slash + 1);
          if (getSassURL(prefix) == null) {
            break;
          }
          String candidate = url.substring(prefix.length());
          URL candidateURL = getSassURL(candidate);
          if (candidateURL != null) {
            resUrl = candidateURL;
            break;
          }
        }

        if (resUrl == null) {
          LOG.warn("Not found: {}", url);
          return null;
        }
      }

      try (InputStream in = resUrl.openStream()) {
        return ImportSuccess.newBuilder().setContents(readUtf8String(in)).setSourceMapUrl(
            "http:/sourcemaps/scss/" + url).build(); // FIXME sourcemap url
      }
    }

    @Override
    public String canonicalize(String url, boolean fromImport) throws Exception {
      if (url.startsWith(CUSTOM_SASS_URI_SCHEME)) {
        return url;
      } else {
        return CUSTOM_SASS_URI_SCHEME + url;
      }
    }
  }

  private static String readUtf8String(InputStream in) throws IOException {
    StringWriter sw = new StringWriter();
    try (InputStreamReader br = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      br.transferTo(sw);
    }
    return sw.toString();
  }

  public void compile(String relativePath, Path scssPath, Path generatedCssPath)
      throws IOException {
    PathUtil.createAncestorDirectories(generatedCssPath);

    Path sourceMapPath = PathUtil.resolveSiblingAppendingSuffix(generatedCssPath, ".map");

    Path tempFile = Files.createTempFile("dumbo-", ".scss");
    mdSupport.renderLiquid(relativePath, scssPath, tempFile.toFile(), true, null);

    String scss = Files.readString(tempFile);

    try (SassCompiler sc = SassCompilerFactory.bundled()) {
      sc.registerImporter(new SassImporter());
      sc.setGenerateSourceMaps(true);
      sc.setSourceMapIncludeSources(false);
      sc.setOutputStyle(OutputStyle.COMPRESSED);
      CompileSuccess cs = sc.compileScssString(scss);

      try (BufferedWriter out = Files.newBufferedWriter(generatedCssPath)) {
        out.write(cs.getCss());
        out.write("\n\n/*# sourceMappingURL=" + PathUtil.relativizeSibling(generatedCssPath,
            sourceMapPath) + " */\n");
      }
      try (BufferedWriter out = Files.newBufferedWriter(sourceMapPath)) {
        out.write(cs.getSourceMap());
      }
      // FIXME need to copy the source files to the sourcemap folder
    } catch (SassCompilationFailedException e) {
      throw new IOException("Cannot compile scss: " + scssPath + ": " + e.getMessage(), e);
    }
  }

  @Override
  public void close() throws IOException {
  }
}
