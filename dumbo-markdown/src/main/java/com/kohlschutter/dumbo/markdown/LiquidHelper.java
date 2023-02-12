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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.dumbo.markdown.liqp.AssetPathTag;
import com.kohlschutter.dumbo.markdown.liqp.DumboIncludeTag;
import com.kohlschutter.dumbo.markdown.liqp.MarkdownifyFilter;
import com.kohlschutter.dumbo.markdown.liqp.NumberOfWordsFilter;
import com.kohlschutter.dumbo.markdown.liqp.SeoTag;
import com.kohlschutter.dumbo.markdown.liqp.SlugifyFilter;
import com.kohlschutter.dumbo.markdown.site.CustomSiteVariables;
import com.kohlschutter.dumbo.markdown.site.PermalinkParser;
import com.kohlschutter.dumbo.markdown.util.PathReaderSupplier;
import com.kohlschutter.stringhold.CachedIOSupplier;
import com.kohlschutter.stringhold.HasExpectedLength;
import com.kohlschutter.stringhold.HasLength;
import com.kohlschutter.stringhold.IOExceptionHandler.ExceptionResponse;
import com.kohlschutter.stringhold.StringHolder;
import com.kohlschutter.stringhold.liqp.StringHolderRenderTransformer;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.sequence.LineAppendable;

import liqp.ParseSettings;
import liqp.RenderSettings;
import liqp.Template;
import liqp.TemplateParser;
import liqp.parser.Flavor;

public class LiquidHelper {
  private static final char[] FRONT_MATTER_LINE = new char[] {'-', '-', '-', '\n'};

  public static final String ENVIRONMENT_KEY_DUMBO_APP = ".dumbo.app";

  // snakeyaml
  private final LoadSettings loadSettings = YAMLSupport.DEFAULT_LOAD_SETTINGS;

  private final TemplateParser liqpParser;

  private final ServerApp app;

  private final Map<String, Object> commonVariables;

  // FIXME revisit this
  static final class GraalVMStub {
    static final MarkdownServlet obj1 = new MarkdownServlet();
    static final HtmlJspServlet obj2 = new HtmlJspServlet();
    static final HtmlRenderer obj3 = HtmlRenderer.builder().build();
    static final LineAppendable.Options[] obj4 = LineAppendable.Options.values();
  }

  LiquidHelper(ServerApp app, Map<String, Object> commonVariables) {
    this.app = app;
    this.commonVariables = commonVariables;
    this.liqpParser = newLiqpParser(app);
  }

  public TemplateParser getLiqpParser() {
    return liqpParser;
  }

  private boolean hasFrontMatter(Reader in) throws IOException {
    boolean haveFrontMatter;
    char[] cbuf = new char[4];
    in.mark(cbuf.length);

    haveFrontMatter = (in.read(cbuf) == cbuf.length && Arrays.equals(cbuf, FRONT_MATTER_LINE));
    in.reset();
    return haveFrontMatter;
  }

  public Object prerenderLiquid(PathReaderSupplier inSup, Map<String, Object> variablesIn,
      Supplier<Map<String, Object>> pageVariablesSupplier) throws IOException {
    return prerenderLiquid(inSup, variablesIn, "page", pageVariablesSupplier);
  }

  public Object prerenderLiquid(PathReaderSupplier inSup, Map<String, Object> variablesIn,
      String collectionItemType, Supplier<Map<String, Object>> itemVariablesSupplier)
      throws IOException {
    int expectedLen;
    if (inSup instanceof HasLength) {
      expectedLen = ((HasLength) inSup).length();
    } else if (inSup instanceof HasExpectedLength) {
      expectedLen = ((HasExpectedLength) inSup).getExpectedLength();
    } else {
      expectedLen = 0;
    }
    return prerenderLiquid(inSup, expectedLen, variablesIn, collectionItemType,
        itemVariablesSupplier);
  }

  public Object prerenderLiquid(PathReaderSupplier inSup, int expectedLen,
      Map<String, Object> variablesIn) throws IOException {
    return prerenderLiquid(inSup, expectedLen, variablesIn, () -> new HashMap<>());
  }

  public Object prerenderLiquid(PathReaderSupplier inSup, int expectedLen,
      Map<String, Object> variables, Supplier<Map<String, Object>> pageVariablesSupplier)
      throws IOException {
    return prerenderLiquid(inSup, expectedLen, variables, "page", pageVariablesSupplier);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> parseFrontMatter(PathReaderSupplier inSup,
      Map<String, Object> variables, String collectionItemType,
      Supplier<Map<String, Object>> pageVariablesSupplier) throws IOException {
    return (Map<String, Object>) prerenderLiquid(inSup, 0, variables, collectionItemType,
        pageVariablesSupplier, true);
  }

  public Object prerenderLiquid(PathReaderSupplier inSup, int expectedLen,
      Map<String, Object> variables, String collectionItemType,
      Supplier<Map<String, Object>> pageVariablesSupplier) throws IOException {
    return prerenderLiquid(inSup, expectedLen, variables, collectionItemType, pageVariablesSupplier,
        false);
  }

  private Object prerenderLiquid(PathReaderSupplier inSup, int expectedLen,
      Map<String, Object> variables, String collectionItemType,
      Supplier<Map<String, Object>> pageVariablesSupplier, boolean justParseFrontMatter)
      throws IOException {

    Reader in = inSup.get();

    if (in instanceof HasLength) {
      expectedLen = ((HasLength) in).length();
    } else if (inSup instanceof HasExpectedLength) {
      expectedLen = ((HasExpectedLength) in).getExpectedLength();
    }

    if (!in.markSupported()) {
      in = new BufferedReader(in);
    }

    if (hasFrontMatter(in)) {
      // front matter enables Liquid templates

      Map<String, Object> pageVariables = pageVariablesSupplier.get();
      if (!justParseFrontMatter || variables != null) {
        Objects.requireNonNull(variables);
        variables.put(collectionItemType, pageVariables);
      }

      initDefaults(inSup.getType(), inSup.getRelativePath(), pageVariables);

      parseFrontMatter(in, pageVariables);

      Object tags = pageVariables.get("tags");
      if (tags instanceof String) {
        tags = new LinkedHashSet<>(Arrays.asList(((String) tags).split("[\\s]+")));
        pageVariables.put("tags", tags);
      }

      String url;
      try {
        CustomSiteVariables.storePathAndFilename(inSup.getRelativePath(), pageVariables);
        if (variables != null) {
          CustomSiteVariables.copyPathAndFileName(pageVariables, variables);
        }

        url = PermalinkParser.parsePermalink((String) pageVariables.get("permalink"),
            pageVariables);
        pageVariables.put("url", url);
      } catch (ParseException e) {
        e.printStackTrace();
      }

      Template template = liqpParser.parse(in);
      for (RuntimeException exc : template.errors()) {
        exc.printStackTrace();
      }

      if (justParseFrontMatter) {
        return pageVariables;
      } else {
        Object obj = template.renderToObject(variables);

        return obj;
      }
    } else {
      if (justParseFrontMatter) {
        return null;
      }
      CompletableFuture<IOException> excHolder = new CompletableFuture<IOException>();
      StringHolder s = StringHolder.withReaderSupplierExpectedLength(expectedLen,
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

  private void initDefaults(String type, String relativePath, Map<String, Object> pageVariables) {
    @SuppressWarnings("unchecked")
    List<Map<String, Map<String, Object>>> cv =
        (List<Map<String, Map<String, Object>>>) ((Map<String, Object>) commonVariables.get("site"))
            .get("defaults");
    if (cv == null || cv.isEmpty()) {
      return;
    }

    for (Map<String, Map<String, Object>> en : cv) {
      Map<String, Object> scope = en.get("scope");

      Object scopeType = scope.get("type");
      if (scopeType != null) {
        if (!scopeType.equals(type) && !String.valueOf(scopeType).isEmpty()) {
          continue;
        }
      }
      Object scopePath = scope.get("path");
      if (scopePath != null) {
        String scopePathStr = String.valueOf(scopePath);
        if (!scopePathStr.isEmpty()) {
          if (scopePathStr.contains("*")) {
            throw new UnsupportedOperationException("globbing is not yet supported");
          }
          if (!scopePathStr.equals(relativePath) && !(relativePath.startsWith(scopePathStr)
              && relativePath.length() > scopePathStr.length() && relativePath.charAt(scopePathStr
                  .length()) == '/')) {
            continue;
          }
        }
      }

      Map<String, Object> values = en.get("values");
      pageVariables.putAll(values);
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
        Map<String, Object> map = (Map<String, Object>) o;
        page.putAll(map);
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
    Set<String> includedLayouts = ((ThreadLocal<Set<String>>) ((Map<String, Object>) variables.get(
        "dumbo")).get("includedLayouts")).get();
    do {
      if (!includedLayouts.add(layoutId)) {
        IOException e = new IOException("Circular reference detected: Layout " + layoutId
            + " already detected: " + includedLayouts);
        e.printStackTrace();
        throw e;
      }

      variables.put("content", contentSupply);

      @SuppressWarnings("unchecked")
      Map<String, Object> layoutVariables = (Map<String, Object>) variables.computeIfAbsent(
          "layout", (k) -> new HashMap<>());
      layoutVariables.remove("layout");

      if (in != null) {
        if (hasFrontMatter(in)) {
          // enables Liquid templates
          parseFrontMatter(in, layoutVariables);
        }

        try {
          Template template = liqpParser.parse(in);
          for (RuntimeException exc : template.errors()) {
            // FIXME handle errors
            exc.printStackTrace();
          }
          contentSupply = template.renderToObjectUnguarded(variables);
        } catch (RuntimeException e) {
          throw new RuntimeException("Error in layout " + layoutId, e);
        }

        in.close();
      }

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
            .with(new MarkdownifyFilter(new MarkdownHelper())) //
            .with(new SlugifyFilter()) //
            .with(new NumberOfWordsFilter())
            // tags
            .with(new DumboIncludeTag()) //
            .with(new SeoTag()) //
            .with(new AssetPathTag()) //
            .build()) //
        .withRenderSettings(new RenderSettings.Builder() //
            // .withRenderTransformer(RenderTransformer.DEFAULT) //
            .withRaiseExceptionsInStrictMode(false).withStrictVariables(true) //
            .withRenderTransformer(StringHolderRenderTransformer.getSharedCacheInstance()) //
            // .withRenderTransformer(StringsOnlyRenderTransformer.getInstance()) //
            .withShowExceptionsFromInclude(true) //
            .withEnvironmentMapConfigurator((env) -> {
              env.put(ENVIRONMENT_KEY_DUMBO_APP, app);
            }).build()) //
        .build();
  }
}
