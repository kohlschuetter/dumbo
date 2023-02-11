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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.ImplementationIdentity;
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

  MarkdownSupportImpl(ServerApp app) throws IOException {
    this.app = app;
    this.liquid = new LiquidHelper(app, commonVariables);
    this.liquidMarkdown = new LiquidMarkdownHelper(liquid);

    commonVariables.put("dumbo", commonDumboVariables);

    siteObject = new SiteObject(app, liquid);
    commonVariables.put("site", siteObject);
    commonVariables.put("jekyll", new JekyllObject());

    siteObject.initCollections();
    commonVariables.put("paginator", new PaginatorObject(commonVariables));

    createFiles();
  }

  @SuppressWarnings("unchecked")
  private void createFiles() throws IOException {
    for (String collectionId : ((Map<String, Object>) siteObject.get("collections")).keySet()) {
      SiteCollection sc = (SiteCollection) siteObject.get(collectionId);
      if (!sc.isOutput()) {
        continue;
      }

      URL webappBaseURL = app.getResource("webapp/");
      if (webappBaseURL == null) {
        throw new IllegalStateException("Cannot get webapp base");
      }

      File webappWorkDir = app.getWebappWorkDir();
      webappWorkDir.mkdirs();

      List<Map<String, Object>> list = (List<Map<String, Object>>) siteObject.get(collectionId);
      for (Map<String, Object> l : list) {
        Map<String, Object> variables = new HashMap<>(commonVariables);
        variables.put("page", l);

        String permalink = (String) l.get("permalink");
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

        if (permalink.endsWith("/")) {
          permalink += "index.html";
        }
        File permalinkFile = new File(webappWorkDir, permalink);
        permalinkFile.getParentFile().mkdirs();

        // System.out.println("- " + permalinkFile + ": <- " + resourceURL);

        String layout = (String) l.get("layout");

        Path path;
        try {
          path = Path.of(resourceURL.toURI());
        } catch (URISyntaxException e) {
          throw new IllegalStateException(e);
        }

        renderMarkdown(null, relativePath, path, permalinkFile, true, layout, collectionId);
      }
    }
  }

  public void renderMarkdown(HttpServletResponse resp, String relativePath, Path mdPath,
      File targetFile, boolean generateHtmlFile, String defaultLayout, String collectionId)
      throws IOException {
    Map<String, Object> variables = new HashMap<>(commonVariables);

    @SuppressWarnings("unchecked")
    Map<String, Object> dumboVariables = (Map<String, Object>) variables.get("dumbo");
    dumboVariables.put("htmlHead", com.kohlschutter.dumbo.JSPSupport.htmlHead(app));
    dumboVariables.put("htmlBodyTop", com.kohlschutter.dumbo.JSPSupport.htmlBodyTop(app));
    dumboVariables.put("includedLayouts", new ThreadLocal<>() {
      public Object get() {
        return new LinkedHashSet<>();
      }
    });

    @SuppressWarnings("unchecked")
    Map<String, Object> pageObj = (Map<String, Object>) variables.computeIfAbsent("page", (k) -> {
      return new HashMap<String, Object>();
    });
    if (defaultLayout == null) {
      defaultLayout = "default";
    }
    pageObj.put("layout", defaultLayout);

    Document document = liquidMarkdown.parseLiquidMarkdown(relativePath, mdPath, variables,
        collectionId);

    long time = System.currentTimeMillis();

    try (SuccessfulCloseWriter mdReloadOut = mdReloadWriter(targetFile, generateHtmlFile)) {
      final Appendable appendable;
      PrintWriter servletOut;
      if (resp != null) {
        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        // do not add to try-catch block, otherwise we won't see error messages
        servletOut = resp.getWriter();

        appendable = SuppressErrorsAppendable.multiplexIfNecessary(servletOut, mdReloadOut);
      } else {
        servletOut = null;

        appendable = Objects.requireNonNull(mdReloadOut);
      }

      String layoutId0 = YAMLSupport.getVariableAsString(variables, "page", "layout");
      if (layoutId0 == null) {
        layoutId0 = defaultLayout;
      }
      String layoutId = layoutId0;

      try (BufferedReader layoutIn = liquid.layoutReader(layoutId)) {
        // the main content
        StringHolderSequence seq = new StringHolderSequence();
        seq.setExpectedLength(document.getTextLength());
        liquidMarkdown.render(document, seq);

        StringHolder contentSupply = seq;

        if (layoutIn != null) {
          // the main content has a layout declared
          StringHolder originalContentSupply = contentSupply;
          contentSupply = StringHolder.withSupplier(() -> liquid.renderLayout(layoutId, layoutIn,
              originalContentSupply, variables), IOExceptionHandler.ILLEGAL_STATE);
        }

        contentSupply.appendTo(appendable);
      }

      if (appendable instanceof SuppressErrorsAppendable) {
        SuppressErrorsAppendable multiplexed = (SuppressErrorsAppendable) appendable;

        if (mdReloadOut != null) {
          mdReloadOut.setSuccessful(!multiplexed.hasError(mdReloadOut));
        }
        multiplexed.checkError(servletOut);
      } else if (mdReloadOut != null) {
        mdReloadOut.setSuccessful(true);
      }
    }

    time = System.currentTimeMillis() - time;
    LOG.info("Request time: " + time + "ms");
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
}
