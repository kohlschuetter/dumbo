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
package com.kohlschutter.dumbo.markdown;

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
import com.kohlschutter.stringhold.CachedIOSupplier;
import com.kohlschutter.stringhold.IOExceptionHandler.ExceptionResponse;
import com.kohlschutter.stringhold.IOSupplier;
import com.kohlschutter.stringhold.StringHolder;

import liqp.Template;
import liqp.TemplateParser;

public class LiquidSupport {
  private static final char[] FRONT_MATTER_LINE = new char[] {'-', '-', '-', '\n'};

  // snakeyaml
  private final LoadSettings loadSettings = LoadSettings.builder().setAllowDuplicateKeys(true)
      .build();

  private final TemplateParser liqpParser;

  private final ServerApp app;

  public LiquidSupport(ServerApp app, TemplateParser liqpParser) {
    this.app = app;
    this.liqpParser = liqpParser;
  }

  public StringHolder prerender(File file, Map<String, Object> variables)
      throws FileNotFoundException, IOException {
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

  public StringHolder prerender(IOSupplier<Reader> inSup, int estimatedLen,
      Map<String, Object> variables) throws IOException {
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

  public StringHolder renderLayout(String layoutId, BufferedReader in, StringHolder contentSupply,
      Map<String, Object> variables) throws IOException {
    @SuppressWarnings("unchecked")
    Set<String> includedLayouts = (Set<String>) ((Map<String, Object>) variables.get("dumbo")).get(
        "includedLayouts");
    do {
      System.out.println("RENDER LAYOUT " + layoutId);
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

      Template template = liqpParser.parse(in);
      System.out.println("PRERENDER " + System.currentTimeMillis());
      contentSupply = template.prerender(variables);
      System.out.println("PRERENDER DONE");

      in.close();

      System.out.println("RENDER LAYOUT DONE " + layoutId);

      // the layout can have another layout
      layoutId = YAMLSupport.getVariableAsString(variables, "layout", "layout");
      in = layoutReader(layoutId);
      if (in == null) {
        break;
      }
    } while (true);

    return contentSupply;
  }
}
