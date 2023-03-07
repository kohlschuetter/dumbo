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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.ImplementationIdentity;
import com.kohlschutter.dumbo.RenderState;
import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.dumbo.markdown.site.CustomSiteVariables;
import com.kohlschutter.dumbo.markdown.site.JekyllObject;
import com.kohlschutter.dumbo.markdown.site.PaginatorObject;
import com.kohlschutter.dumbo.markdown.site.PermalinkParser;
import com.kohlschutter.dumbo.markdown.site.SiteCollection;
import com.kohlschutter.dumbo.markdown.site.SiteObject;
import com.kohlschutter.dumbo.util.MultiplexedAppendable.SuppressErrorsAppendable;
import com.kohlschutter.dumbo.util.SuccessfulCloseWriter;
import com.kohlschutter.stringhold.IOExceptionHandler;
import com.kohlschutter.stringhold.StringHolder;
import com.kohlschutter.stringhold.StringHolderSequence;
import com.vladsch.flexmark.util.ast.Document;

import jakarta.servlet.http.HttpServletResponse;

final class MarkdownSupportImpl {
  static final ImplementationIdentity<MarkdownSupportImpl> COMPONENT_IDENTITY =
      new ImplementationIdentity<>();

  private static final Logger LOG = LoggerFactory.getLogger(MarkdownSupportImpl.class);

  private final ServerApp app;

  private final Map<String, Object> commonVariables = new HashMap<>();
  private final Map<String, Object> commonDumboVariables = new HashMap<>();

  private final LiquidHelper liquid;
  private final LiquidMarkdownHelper liquidMarkdown;

  private final SiteObject siteObject;

  private final URL webappBaseURL;
  private final File webappWorkDir;

  MarkdownSupportImpl(ServerApp app) throws IOException {
    this.app = app;
    this.liquid = new LiquidHelper(app, commonVariables);
    this.liquidMarkdown = new LiquidMarkdownHelper(liquid);

    webappBaseURL = app.getResource("webapp/");
    if (webappBaseURL == null) {
      throw new IllegalStateException("Cannot get webapp base");
    }

    webappWorkDir = app.getWebappWorkDir();
    webappWorkDir.mkdirs();

    commonVariables.put(LiquidVariables.DUMBO, commonDumboVariables);

    commonVariables.put(LiquidVariables.JEKYLL, new JekyllObject());

    siteObject = SiteObject.addTo(app, liquid, commonVariables);
    commonVariables.put(LiquidVariables.PAGINATOR, new PaginatorObject(commonVariables));

    createFiles();
  }

  @SuppressWarnings("unchecked")
  private void createFiles() throws IOException {
    Map<String, Map<String, Collection<Object>>> categoryArchives = new HashMap<>();
    Map<String, Map<String, Collection<Object>>> tagArchives = new HashMap<>();
    Map<String, Map<String, Map<String, Collection<Object>>>> archives = new HashMap<>();
    archives.put("tags", tagArchives);
    archives.put("categories", categoryArchives);

    for (String collectionId : ((Map<String, Object>) siteObject.get("collections")).keySet()) {
      SiteCollection sc = (SiteCollection) siteObject.get(collectionId);
      if (!sc.isOutput()) {
        continue;
      }

      List<Map<String, Object>> list = (List<Map<String, Object>>) siteObject.get(collectionId);
      for (Map<String, Object> l : list) {
        for (String category : (Collection<String>) nullToEmptyCollection(l.get(
            LiquidVariables.PAGE_CATEGORIES))) {
          categoryArchives.computeIfAbsent(category, (k) -> new HashMap<>()).computeIfAbsent(
              collectionId, (id) -> new ArrayList<>()).add(l);
        }
        for (String tag : (Collection<String>) nullToEmptyCollection(l.get(
            LiquidVariables.PAGE_TAGS))) {
          tagArchives.computeIfAbsent(tag, (k) -> new HashMap<>()).computeIfAbsent(collectionId, (
              id) -> new ArrayList<>()).add(l);
        }
      }
    }
    createArchives(archives);

    siteObject.initCategoriesAndTags(archives);

    for (String collectionId : ((Map<String, Object>) siteObject.get("collections")).keySet()) {
      SiteCollection sc = (SiteCollection) siteObject.get(collectionId);
      if (!sc.isOutput()) {
        continue;
      }

      List<Map<String, Object>> list = (List<Map<String, Object>>) siteObject.get(collectionId);
      for (Map<String, Object> l : list) {
        Map<String, Object> variables = new HashMap<>(commonVariables);
        variables.put(LiquidVariables.PAGE, l);

        String permalink = (String) l.get(LiquidVariables.PAGE_PERMALINK);
        if (permalink == null || permalink.isBlank()) {
          System.err.println("Skipping entry without permalink");
          continue;
        }

        String relativePath = (String) l.get(CustomSiteVariables.DUMBO_RELATIVE_PATH);
        if (relativePath == null) {
          System.err.println("Skipping entry without relative path");
          continue;
        }

        URL resourceURL = app.getResource("markdown/" + relativePath);
        if (resourceURL == null) {
          System.err.println("Skipping entry without resourceURL: " + relativePath);
          continue;
        }

        try {
          permalink = PermalinkParser.parsePermalink(permalink, l);
        } catch (ParseException e) {
          e.printStackTrace();
          continue;
        }

        renderMarkdownPage(permalink, resourceURL, relativePath, collectionId, l);
      }
    }
  }

  private void renderMarkdownPage(String permalink, URL resourceURL, String relativePath,
      String collectionId, Map<String, Object> pageVariables) throws IOException {
    if (permalink.endsWith("/")) {
      permalink += "index.html";
    }
    File permalinkFile = new File(webappWorkDir, permalink);
    permalinkFile.getParentFile().mkdirs();

    String layout = (String) pageVariables.get(LiquidVariables.PAGE_LAYOUT);

    Path path;
    try {
      path = resourceURL == null ? null : Path.of(resourceURL.toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }

    renderMarkdown(relativePath, path, permalinkFile, true, null, layout, collectionId, Collections
        .singletonMap(LiquidVariables.PAGE, pageVariables));
  }

  private static Collection<?> nullToEmptyCollection(Object obj) {
    if (obj == null) {
      return Collections.emptyList();
    } else if (obj instanceof String) {
      if (((String) obj).isEmpty()) {
        return Collections.emptyList();
      } else {
        return Collections.singleton(obj);
      }
    } else if (obj instanceof Object[]) {
      return Arrays.asList((Object[]) obj);
    } else {
      return ((Collection<?>) obj);
    }
  }

  public void renderMarkdown(String relativePath, Path mdPath, File targetFile,
      boolean generateHtmlFile, @Nullable HttpServletResponse resp, @Nullable String defaultLayout,
      @Nullable String collectionId) throws IOException {
    renderMarkdown(relativePath, mdPath, targetFile, generateHtmlFile, resp, defaultLayout,
        collectionId, null);
  }

  public void renderMarkdown(String relativePath, Path mdPath, File targetFile,
      boolean generateHtmlFile, @Nullable HttpServletResponse resp, @Nullable String defaultLayout,
      @Nullable String collectionId, Map<String, Object> variablesOverride) throws IOException {

    Map<String, Object> variables = new HashMap<>(commonVariables);
    if (variablesOverride != null) {
      variables.putAll(variablesOverride);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> dumboVariables = (Map<String, Object>) variables.get(LiquidVariables.DUMBO);
    dumboVariables.put(LiquidVariables.DUMBO_HTMLHEAD,
        com.kohlschutter.dumbo.ExtensionResourceHelper.htmlHead(app));
    dumboVariables.put(LiquidVariables.DUMBO_HTMLBODYTOP,
        com.kohlschutter.dumbo.ExtensionResourceHelper.htmlBodyTop(app));

    ThreadLocal<RenderState> stateTL = RenderState.getThreadLocal();
    stateTL.remove();
    dumboVariables.put(LiquidVariables.DUMBO_STATE_TL, stateTL);

    RenderState state = RenderState.get();
    state.setRelativePath(relativePath);
    state.setApp(app);

    @SuppressWarnings("unchecked")
    Map<String, Object> pageObj = (Map<String, Object>) variables.computeIfAbsent(
        LiquidVariables.PAGE, (k) -> {
          return new HashMap<String, Object>();
        });
    if (defaultLayout == null) {
      defaultLayout = "default";
    }
    if (defaultLayout != null && !pageObj.containsKey(LiquidVariables.PAGE_LAYOUT)) {
      pageObj.put(LiquidVariables.PAGE_LAYOUT, defaultLayout);
    }
    pageObj.put(LiquidVariables.PAGE_COLLECTION, collectionId);

    Document document = liquidMarkdown.parseLiquidMarkdown(relativePath, mdPath, variables,
        collectionId);

    long time = System.currentTimeMillis();

    try (SuccessfulCloseWriter mdReloadOut = mdReloadWriter(targetFile, generateHtmlFile)) {
      final Appendable out;
      PrintWriter servletOut;
      if (resp != null) {
        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        // do not add to try-catch block, otherwise we won't see error messages
        servletOut = resp.getWriter();

        out = SuppressErrorsAppendable.multiplexIfNecessary(servletOut, mdReloadOut);
      } else {
        servletOut = null;

        out = Objects.requireNonNull(mdReloadOut);
      }

      String layoutId0 = YAMLSupport.getVariableAsString(variables, LiquidVariables.PAGE,
          LiquidVariables.PAGE_LAYOUT);
      if (layoutId0 == null) {
        layoutId0 = defaultLayout;
      }
      String layoutId = layoutId0;

      try (BufferedReader layoutIn = liquid.layoutReader(layoutId)) {
        // render the main content first
        StringHolderSequence seq = new StringHolderSequence();
        seq.setExpectedLength(document.getTextLength());
        liquidMarkdown.render(document, seq);

        // // ALTERNATIVE: defer rendering (this causes the outermost layout to be rendered first)
        // StringHolder contentSupply =
        // StringHolder.withSupplierExpectedLength(document.getTextLength(), ()->{
        // StringHolderSequence seq = new StringHolderSequence();
        // liquidMarkdown.render(document, seq);
        // return seq;
        // });

        StringHolder contentSupply = seq;

        if (layoutIn != null) {
          // the main content has a layout declared
          StringHolder originalContentSupply = contentSupply;
          contentSupply = StringHolder.withSupplier(() -> liquid.renderLayout(layoutId, layoutIn,
              originalContentSupply, variables), IOExceptionHandler.ILLEGAL_STATE);
        }

        contentSupply.appendTo(out);
      }

      if (out instanceof SuppressErrorsAppendable) {
        SuppressErrorsAppendable multiplexed = (SuppressErrorsAppendable) out;

        if (mdReloadOut != null) {
          mdReloadOut.setSuccessful(!multiplexed.hasError(mdReloadOut));
        }
        multiplexed.checkError(servletOut);
      } else if (mdReloadOut != null) {
        mdReloadOut.setSuccessful(true);
      }
    }

    time = System.currentTimeMillis() - time;
    LOG.info("Request time: " + time + "ms for " + relativePath);
  }

  private SuccessfulCloseWriter mdReloadWriter(File targetFile, boolean generateHtmlFile)
      throws IOException {
    if (targetFile == null) {
      return null;
    }
    String filename = targetFile.getName();
    int suffix = filename.indexOf('.');
    if (suffix == -1) {
      filename += ".html";
    } else {
      filename = filename.substring(0, suffix) + ".html";
    }

    File htmlFile = new File(targetFile.getParentFile(), filename);
    System.out.println("WD " + app.getWorkDir() + " " + app.getWorkDir().exists());

    if (generateHtmlFile || !htmlFile.exists()) {
      LOG.info("Generating " + htmlFile);
      File mdFileHtmlTmp = File.createTempFile(".md", ".tmp", app.getWorkDir());

      return new SuccessfulCloseWriter(new OutputStreamWriter(new FileOutputStream(mdFileHtmlTmp),
          StandardCharsets.UTF_8)) {

        @Override
        protected void onClosed(boolean success) throws IOException {
          if (success && mdFileHtmlTmp.renameTo(htmlFile)) {
            LOG.debug("renamed successfully: " + htmlFile);
          } else {
            LOG.warn("Failed to create " + htmlFile);
            mdFileHtmlTmp.delete();
          }
        }
      };
    } else {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private void createArchives(Map<String, Map<String, Map<String, Collection<Object>>>> archivesMap)
      throws IOException {
    Map<String, Object> archivesConfig = (Map<String, Object>) siteObject.get(
        LiquidVariables.JEKYLLARCHIVES);
    if (archivesConfig == null || archivesConfig.isEmpty()) {
      return;
    }

    Object enabledObj = archivesConfig.get(LiquidVariables.JEKYLLARCHIVES_ENABLED);
    if (enabledObj == null) {
      return;
    } else if (enabledObj instanceof String) {
      if ("all".equals(enabledObj)) {
        enabledObj = List.of("year", "month", "day", "categories", "tags");
      } else {
        LOG.warn("Illegal string value for liquid-archives enabled: " + enabledObj);
      }
    }

    Collection<String> enabled = (Collection<String>) enabledObj;
    if (enabled.isEmpty()) {
      return;
    }

    Map<String, String> permalinks = (Map<String, String>) archivesConfig.get(
        LiquidVariables.JEKYLLARCHIVES_PERMALINKS);
    if (permalinks == null || permalinks.isEmpty()) {
      return;
    }

    String defaultLayout = (String) archivesConfig.get(LiquidVariables.JEKYLLARCHIVES_LAYOUT);

    Map<String, String> layouts = (Map<String, String>) archivesConfig.get(
        LiquidVariables.JEKYLLARCHIVES_LAYOUTS);
    if (layouts == null) {
      layouts = Collections.emptyMap();
    }

    for (String collectionId : enabled) {
      Map<String, Map<String, Collection<Object>>> map = archivesMap.get(collectionId);
      if (map == null || map.isEmpty()) {
        continue;
      }
      String type = collectionIdSingular(collectionId);

      String permalinkTemplate = permalinks.get(type);
      if (permalinkTemplate == null || permalinkTemplate.isEmpty()) {
        continue;
      }

      for (Map.Entry<String, Map<String, Collection<Object>>> en : map.entrySet()) {
        String name = en.getKey();

        Map<String, Object> pageVariables = new HashMap<>();
        pageVariables.put(LiquidVariables.PAGE_TYPE, type);
        pageVariables.put(LiquidVariables.PAGE_NAME, name);
        pageVariables.put(LiquidVariables.PAGE_TITLE, name); // FIXME?
        pageVariables.put(LiquidVariables.PAGE, pageVariables);

        String layout = layouts.get(type);
        if (layout == null) {
          layout = defaultLayout;
        }
        pageVariables.put(LiquidVariables.PAGE_LAYOUT, layout);

        String permalink;
        try {
          permalink = PermalinkParser.parsePermalink(permalinkTemplate, pageVariables);
        } catch (ParseException e) {
          LOG.warn("Cannot parse permalink: " + permalinkTemplate, e);
          continue;
        }
        pageVariables.putAll(en.getValue());

        renderMarkdownPage(permalink, null, null, collectionId, pageVariables);
      }
    }
  }

  private static String collectionIdSingular(String k) {
    if ("categories".equals(k)) {
      return "category";
    } else if ("tags".equals(k)) {
      return "tag";
    } else {
      return k;
    }
  }
}
