package com.kohlschutter.dumbo.markdown;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;

import liqp.Template;
import liqp.TemplateParser;

public class LiquidMarkdownSupport {
  private static final char[] FRONT_MATTER_LINE = new char[] {'-', '-', '-', '\n'};

  private final TemplateParser liqpParser;

  // flexmark-java
  private final Parser parser;
  private final HtmlRenderer renderer;

  // snakeyaml
  private final LoadSettings loadSettings = LoadSettings.builder().setAllowDuplicateKeys(true)
      .build();

  public LiquidMarkdownSupport(TemplateParser liqpParser) {
    // Liqp
    this.liqpParser = liqpParser;

    // flexmark-java
    this.parser = Parser.builder().build();
    this.renderer = HtmlRenderer.builder().build();
  }

  public Document parse(File mdFile, Map<String, Object> variables, boolean parseMarkup) throws FileNotFoundException,
      IOException {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(mdFile),
        StandardCharsets.UTF_8))) {
      return parse(in, variables, parseMarkup);
    }
  }

  public Document parse(Reader in, Map<String, Object> variables, boolean parseMarkup) throws IOException {
    boolean haveFrontMatter;
    char[] cbuf = new char[4];
    in.mark(cbuf.length);

    haveFrontMatter = (in.read(cbuf) == cbuf.length && Arrays.equals(cbuf, FRONT_MATTER_LINE));
    in.reset();

    Reader flexmarkIn;
    if (haveFrontMatter) {
      // enables Liquid templates
      Map<String, Object> pageVariables = new HashMap<>();
      variables.put("page", pageVariables);

      parseFrontMatter(in, pageVariables);

      Template parse = liqpParser.parse(in);
      flexmarkIn = new StringReader(parse.render(variables));
    } else {
      flexmarkIn = in;
    }

    return parser.parseReader(flexmarkIn);
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

  public String render(Document document) {
    return renderer.render(document);
  }

  public void render(Document document, Appendable appendable) {
    renderer.render(document, appendable);
  }
}
