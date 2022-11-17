package com.kohlschutter.dumbo;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import liqp.ProtectionSettings;
import liqp.RenderSettings;
import liqp.Template;

public class MarkdownServlet extends HttpServlet {
  private static final char[] FRONT_MATTER_LINE = new char[] {'-', '-', '-', '\n'};
  private static final long serialVersionUID = 1L;
  private ServletContext servletContext;

  private ServerApp app;

  // snakeyaml
  private LoadSettings loadSettings;

  // liqp
  private ProtectionSettings protectionSettings;
  private RenderSettings renderSettings;

  // flexmark-java
  private Parser parser;
  private HtmlRenderer renderer;

  private final Map<String, Object> commonVariables = new HashMap<>();
  private final Map<String, Object> commonDumboVariables = new HashMap<>();

  @Override
  public void init() throws ServletException {
    servletContext = getServletContext();

    this.app = AppHTTPServer.getServerApp(servletContext);

    // snakeyaml
    loadSettings = LoadSettings.builder().setAllowDuplicateKeys(true).build();

    // liqp
    protectionSettings = new ProtectionSettings.Builder().build();
    renderSettings = new RenderSettings.Builder().build();

    // flexmark-java
    parser = Parser.builder().build();
    renderer = HtmlRenderer.builder().build();

    commonVariables.clear();
    commonDumboVariables.clear();
    commonVariables.put("dumbo", commonDumboVariables);
  }

  @SuppressWarnings("unchecked")
  private void parseFrontMatter(BufferedReader in, Map<String, Object> page) throws IOException {
    StringBuilder sbFrontMatter = new StringBuilder();
    sbFrontMatter.append(in.readLine());
    sbFrontMatter.append('\n');
    String l;
    do {
      l = in.readLine();
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

  private String getVariableAsString(Map<String, Object> variables, String... pathElements) {
    Object obj = variables;
    for (String pathElement : pathElements) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) obj;
      obj = map.get(pathElement);
      if (obj == null) {
        return null;
      }
    }
    boolean loop;
    do {
      loop = false;
      if (obj instanceof Collection) {
        obj = ((Collection<?>) obj).iterator().next();
        loop = true;
      }
      if (obj instanceof Map) {
        obj = ((Map<?, ?>) obj).values();
        loop = true;
      }
      if (obj == null) {
        return null;
      }
    } while (loop);
    return obj.toString();
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

    Map<String, Object> page = new HashMap<>();
    variables.put("page", page);

    CharBuffer cb;
    boolean haveFrontMatter;
    try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(mdFile),
        StandardCharsets.UTF_8))) {
      char[] cbuf = new char[4];
      in.mark(cbuf.length);

      haveFrontMatter = (in.read(cbuf) == cbuf.length && Arrays.equals(cbuf, FRONT_MATTER_LINE));
      in.reset();

      if (haveFrontMatter) {
        parseFrontMatter(in, page);
      }

      cb = CharBuffer.allocate((int) mdFileLength);
      in.read(cb);
    }
    cb.flip();

    String markdown = cb.toString();

    final String layout;
    if (haveFrontMatter) {
      // enables Liquid templates
      Template parse = Template.parse(markdown).withProtectionSettings(protectionSettings)
          .withRenderSettings(renderSettings);
      markdown = parse.render(variables);

      layout = getVariableAsString(variables, "page", "layout");
    } else {
      layout = null;
    }

    Document document = parser.parse(markdown);

    resp.setContentType("text/html;charset=UTF-8");
    resp.setCharacterEncoding("UTF-8");
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {
      String output = renderer.render(document);

      if (layout != null && !layout.isBlank() && app != null) {
        URL layoutURL = app.getResource("markdown/_layouts/" + layout + ".html");
        if (layoutURL != null) {
          variables.put("content", output);

          try (InputStream in = layoutURL.openStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Template parse = Template.parse(output).withProtectionSettings(protectionSettings)
                .withRenderSettings(renderSettings);
            output = parse.render(variables);
          }
        }
      }
      writer.append(output);
      writer.flush();

      if (path.endsWith(".md")) {
        String htmlPath = path.substring(0, path.length() - ".md".length()) + ".html";

        File htmlFile = new File(htmlPath);

        if ("true".equals(req.getParameter("reload")) /* || !htmlFile.exists() */) {
          File mdFileHtmlTmp = File.createTempFile(".md", ".tmp", htmlFile.getParentFile());
          System.out.println("Generating " + htmlFile);
          boolean success = false;
          try (FileOutputStream fout = new FileOutputStream(mdFileHtmlTmp)) {
            bos.writeTo(fout);
            success = true;
          } finally {
            if (success && mdFileHtmlTmp.renameTo(htmlFile)) {
              // renamed
              // System.out.println("took " + (System.currentTimeMillis() - time) + "ms");
            } else {
              System.err.println("Failed to create " + htmlFile);
              mdFileHtmlTmp.delete();
            }
          }
        }
      }

      resp.setContentLength(bos.size());
      try (ServletOutputStream out = resp.getOutputStream()) {
        if (!out.isReady() && "HEAD".equals(req.getMethod())) {
          return;
        }
        bos.writeTo(out);
        out.flush();
        resp.flushBuffer();
      }
    }
  }
}
