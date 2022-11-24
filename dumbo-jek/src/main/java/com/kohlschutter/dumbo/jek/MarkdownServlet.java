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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.dumbo.AppHTTPServer;
import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.dumbo.util.MultiplexedAppendable.SuppressErrorsAppendable;
import com.kohlschutter.dumbo.util.SuccessfulCloseWriter;
import com.kohlschutter.stringhold.StringHolder;
import com.vladsch.flexmark.util.ast.Document;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MarkdownServlet extends HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger(MarkdownServlet.class);
  private static final long serialVersionUID = 1L;
  private ServletContext servletContext;

  private ServerApp app;

  private LiquidSupport liquid;
  private LiquidMarkdownSupport liquidMarkdown;

  private final Map<String, Object> commonVariables = new HashMap<>();
  private final Map<String, Object> commonDumboVariables = new HashMap<>();

  @Override
  public void init() throws ServletException {
    servletContext = getServletContext();

    this.app = AppHTTPServer.getServerApp(servletContext);

    this.liquid = new LiquidSupport(app);
    this.liquidMarkdown = new LiquidMarkdownSupport(liquid);

    commonVariables.clear();
    commonDumboVariables.clear();
    commonVariables.put("dumbo", commonDumboVariables);

    commonVariables.put("site", SiteConfig.init(app));
  }

  private SuccessfulCloseWriter mdReloadWriter(String path, HttpServletRequest req)
      throws IOException {
    if (!path.endsWith(".md")) {
      return null;
    }

    String htmlPath = path.substring(0, path.length() - ".md".length()) + ".html";

    File htmlFile = new File(htmlPath);

    if ("true".equals(req.getParameter("reload")) /* || !htmlFile.exists() */) {
      File mdFileHtmlTmp = File.createTempFile(".md", ".tmp", htmlFile.getParentFile());
      LOG.info("Generating " + htmlFile);

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

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    try {
      doGet0(req, resp);
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected void doGet0(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {

    String path = servletContext.getRealPath(req.getServletPath());
    if (path == null) {
      return;
    }
    File mdFile = new File(path);
    if (!mdFile.exists() || mdFile.isDirectory()) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    long mdFileLength = mdFile.length();
    if (mdFileLength > Integer.MAX_VALUE) {
      // FIXME: use a lower bound
      resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      return;
    }

    // long time = System.currentTimeMillis();

    Map<String, Object> variables = new HashMap<>(commonVariables);

    @SuppressWarnings("unchecked")
    Map<String, Object> dumboVariables = (Map<String, Object>) variables.get("dumbo");
    dumboVariables.put("htmlHead", com.kohlschutter.dumbo.JSPSupport.htmlHead(req.getSession()));
    dumboVariables.put("htmlBodyTop", com.kohlschutter.dumbo.JSPSupport.htmlBodyTop(req
        .getSession()));
    dumboVariables.put("includedLayouts", new LinkedHashSet<>());

    Document document = liquidMarkdown.parse(mdFile, variables);

    resp.setContentType("text/html;charset=UTF-8");
    resp.setCharacterEncoding("UTF-8");

    // do not add to try-catch block, otherwise we won't see error messages
    PrintWriter servletOut = resp.getWriter();

    long time = System.currentTimeMillis();

    try (SuccessfulCloseWriter mdReloadOut = mdReloadWriter(path, req)) {
      final Appendable appendable = SuppressErrorsAppendable.multiplexIfNecessary(servletOut,
          mdReloadOut);

      String layoutId = YAMLSupport.getVariableAsString(variables, "page", "layout");
      try (BufferedReader layoutIn = liquid.layoutReader(layoutId)) {
        // the main content
        Object contentSupply = StringHolder.withSupplierExpectedLength(document.getTextLength(),
            () -> liquidMarkdown.render(document));

        if (layoutIn != null) {
          // the main content has a layout declared
          contentSupply = liquid.renderLayout(layoutId, layoutIn, contentSupply, variables);
        }

        if (contentSupply instanceof StringHolder) {
          ((StringHolder) contentSupply).appendTo(appendable);
        } else {
          appendable.append(contentSupply.toString());
        }
      }

      if (appendable instanceof SuppressErrorsAppendable) {
        SuppressErrorsAppendable multiplexed = (SuppressErrorsAppendable) appendable;

        if (mdReloadOut != null) {
          mdReloadOut.setSuccessful(!multiplexed.hasError(mdReloadOut));
        }
        multiplexed.checkError(servletOut);
      }
    }

    time = System.currentTimeMillis() - time;
    LOG.info("Request time: " + time + "ms");
  }
}
