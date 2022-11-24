/*
 * Copyright 2022 Christian Kohlschütter
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
package com.kohlschutter.dumbo.jek;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.dumbo.jek.liqp.filters.Markdownify;
import com.kohlschutter.dumbo.jek.liqp.filters.NumberOfWords;
import com.kohlschutter.dumbo.jek.liqp.filters.Slugify;
import com.kohlschutter.dumbo.jek.liqp.tags.AssetPath;
import com.kohlschutter.dumbo.jek.liqp.tags.DumboInclude;
import com.kohlschutter.dumbo.jek.liqp.tags.Seo;
import com.kohlschutter.stringhold.CachedIOSupplier;
import com.kohlschutter.stringhold.IOExceptionHandler.ExceptionResponse;
import com.kohlschutter.stringhold.IOSupplier;
import com.kohlschutter.stringhold.StringHolder;

import liqp.ParseSettings;
import liqp.RenderSettings;
import liqp.RenderSettings.RenderMode;
import liqp.Template;
import liqp.TemplateParser;
import liqp.parser.Flavor;

public class LiquidSupport {
  private static final char[] FRONT_MATTER_LINE = new char[] {'-', '-', '-', '\n'};

  public static final String ENVIRONMENT_KEY_DUMBO_APP = ".dumbo.app";

  // snakeyaml
  private final LoadSettings loadSettings = YAMLSupport.DEFAULT_LOAD_SETTINGS;

  private final TemplateParser liqpParser;

  private final ServerApp app;

  public LiquidSupport(ServerApp app) {
    this.app = app;
    this.liqpParser = newLiqpParser(app);
  }

  public TemplateParser getLiqpParser() {
    return liqpParser;
  }

  public Object prerender(File file, Map<String, Object> variables) throws FileNotFoundException,
      IOException {
    return prerender(() -> new BufferedReader(new InputStreamReader(new FileInputStream(file),
        StandardCharsets.UTF_8)), (int) file.length(), variables);
  }

  private boolean checkFrontMatter(Reader in) throws IOException {
    boolean haveFrontMatter;
    char[] cbuf = new char[4];
    in.mark(cbuf.length);

    haveFrontMatter = (in.read(cbuf) == cbuf.length && Arrays.equals(cbuf, FRONT_MATTER_LINE));
    in.reset();
    return haveFrontMatter;
  }

  public Object prerender(IOSupplier<Reader> inSup, int estimatedLen, Map<String, Object> variables)
      throws IOException {
    Reader in = inSup.get();

    if (checkFrontMatter(in)) {
      // enables Liquid templates
      Map<String, Object> pageVariables = new HashMap<>();
      variables.put("page", pageVariables);
      parseFrontMatter(in, pageVariables);

      Template parse = liqpParser.parse(in);
      return parse.prerender(variables);
    } else {
      CompletableFuture<IOException> excHolder = new CompletableFuture<IOException>();
      StringHolder s = StringHolder.withReaderSupplierExpectedLength(estimatedLen,
          new CachedIOSupplier<>(in, inSup), (IOException e) -> {
            excHolder.complete(e);
            return ExceptionResponse.EXCEPTION_MESSAGE;
          });
      IOException exception = excHolder.getNow(null);
      if (exception != null) {
        throw exception;
      }
      return s;
    }
  }

  @SuppressWarnings("unchecked")
  private void parseFrontMatter(Reader in, Map<String, Object> page) throws IOException {
    BufferedReader br = (in instanceof BufferedReader) ? (BufferedReader) in : new BufferedReader(
        in);

    StringBuilder sbFrontMatter = new StringBuilder();
    sbFrontMatter.append(br.readLine());
    sbFrontMatter.append('\n');
    String l;
    do {
      l = br.readLine();
      if (l == null) {
        break;
      }
      sbFrontMatter.append(l);
      sbFrontMatter.append('\n');
    } while (!"---".equals(l));

    Iterable<Object> loadAllFromString = new Load(loadSettings).loadAllFromString(sbFrontMatter
        .toString());
    for (Object o : loadAllFromString) {
      if (o == null) {
        continue;
      } else if (o instanceof Map) {
        page.putAll((Map<String, Object>) o);
      } else {
        System.err.println("Unexpected YAML object class in front matter: " + o.getClass());
      }
    }
  }

  public BufferedReader layoutReader(String layout) throws IOException {
    if (layout == null || layout.isBlank() || app == null) {
      return null;
    }
    URL layoutURL = app.getResource("markdown/_layouts/" + layout + ".html");
    if (layoutURL == null) {
      return null;
    } else {
      return new BufferedReader(new InputStreamReader(layoutURL.openStream(),
          StandardCharsets.UTF_8));
    }
  }

  public Object renderLayout(String layoutId, BufferedReader in, Object contentSupply,
      Map<String, Object> variables) throws IOException {
    @SuppressWarnings("unchecked")
    Set<String> includedLayouts = (Set<String>) ((Map<String, Object>) variables.get("dumbo")).get(
        "includedLayouts");
    do {
      if (!includedLayouts.add(layoutId)) {
        throw new IOException("Circular reference detected: Layout " + layoutId
            + " already detected: " + includedLayouts);
      }

      variables.put("content", contentSupply);

      @SuppressWarnings("unchecked")
      Map<String, Object> layoutVariables = (Map<String, Object>) variables.computeIfAbsent(
          "layout", (k) -> new HashMap<>());
      layoutVariables.remove("layout");

      if (checkFrontMatter(in)) {
        // enables Liquid templates
        parseFrontMatter(in, layoutVariables);
      }

      contentSupply = liqpParser.parse(in).prerenderUnguarded(variables);

      in.close();

      // the layout can have another layout
      layoutId = YAMLSupport.getVariableAsString(variables, "layout", "layout");
      in = layoutReader(layoutId);
      if (in == null) {
        break;
      }
    } while (true);

    return contentSupply;
  }

  public static TemplateParser newLiqpParser(ServerApp app) {
    return new TemplateParser.Builder() //
        .withParseSettings(new ParseSettings.Builder() //
            .with(Flavor.JEKYLL.defaultParseSettings()) //
            // filters
            .with(new Markdownify()) //
            .with(new Slugify()) //
            .with(new NumberOfWords())
            // tags
            .with(new DumboInclude()) //
            .with(new Seo()) //
            .with(new AssetPath()) //
            .build()) //
        .withRenderSettings(new RenderSettings.Builder() //
            // .withRenderMode(RenderMode.STRINGHOLDER) //
            .withRenderMode(RenderMode.STRING) //
            .withShowExceptionsFromInclude(true) //
            .withEnvironmentMapConfigurator((env) -> {
              env.put(ENVIRONMENT_KEY_DUMBO_APP, app);
            }).build()) //
        .build();
  }
}
