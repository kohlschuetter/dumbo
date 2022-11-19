package com.kohlschutter.dumbo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.kohlschutter.dumbo.markdown.LiquidMarkdownSupport;
import com.kohlschutter.dumbo.markdown.MarkdownConfig;
import com.kohlschutter.dumbo.markdown.YAMLSupport;
import com.kohlschutter.dumbo.util.MultiplexedAppendable.SuppressErrorsAppendable;
import com.kohlschutter.dumbo.util.SuccessfulCloseWriter;
import com.vladsch.flexmark.util.ast.Document;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import liqp.Template;
import liqp.TemplateParser;

public class MarkdownServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private ServletContext servletContext;

  private ServerApp app;

  private LiquidMarkdownSupport liquidMarkdown;

  private TemplateParser liqpParser = MarkdownConfig.LIQP_PARSER;

  private final Map<String, Object> commonVariables = new HashMap<>();
  private final Map<String, Object> commonDumboVariables = new HashMap<>();

  @Override
  public void init() throws ServletException {
    servletContext = getServletContext();

    this.app = AppHTTPServer.getServerApp(servletContext);

    this.liquidMarkdown = new LiquidMarkdownSupport(liqpParser);

    commonVariables.clear();
    commonDumboVariables.clear();
    commonDumboVariables.put(".app", app);
    commonVariables.put("dumbo", commonDumboVariables);
  }

  private InputStream layoutStream(String layout) throws IOException {
    if (layout == null || layout.isBlank() || app == null) {
      return null;
    }
    URL layoutURL = app.getResource("markdown/_layouts/" + layout + ".html");
    if (layoutURL == null) {
      return null;
    } else {
      return layoutURL.openStream();
    }
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
      System.out.println("Generating " + htmlFile);

      return new SuccessfulCloseWriter(new OutputStreamWriter(new FileOutputStream(mdFileHtmlTmp),
          StandardCharsets.UTF_8)) {

        @Override
        protected void onClosed(boolean success) throws IOException {
          if (success && mdFileHtmlTmp.renameTo(htmlFile)) {
            System.out.println("renamed successfully: " + htmlFile);
          } else {
            System.err.println("Failed to create " + htmlFile);
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

    Document document = liquidMarkdown.parse(mdFile, variables, true);

    resp.setContentType("text/html;charset=UTF-8");
    resp.setCharacterEncoding("UTF-8");

    try (PrintWriter servletOut = resp.getWriter();
        SuccessfulCloseWriter mdReloadOut = mdReloadWriter(path, req)) {

      final Appendable appendable = SuppressErrorsAppendable.multiplexIfNecessary(servletOut,
          mdReloadOut);

      try (InputStream layoutStream = layoutStream(YAMLSupport.getVariableAsString(variables,
          "page", "layout"))) {
        String content = liquidMarkdown.render(document);

        if (layoutStream != null) {
          variables.clear();
          variables.putAll(commonVariables);
          variables.put("content", content);
          Template template = liqpParser.parse(layoutStream);

          content = template.render(variables);
        }
        appendable.append(content);
      }

      if (appendable instanceof SuppressErrorsAppendable) {
        SuppressErrorsAppendable multiplexed = (SuppressErrorsAppendable) appendable;

        if (mdReloadOut != null) {
          mdReloadOut.setSuccessful(!multiplexed.hasError(mdReloadOut));
        }
        multiplexed.checkError(servletOut);
      }
    }
  }
}
