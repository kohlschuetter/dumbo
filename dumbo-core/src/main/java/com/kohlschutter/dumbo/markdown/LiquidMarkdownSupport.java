package com.kohlschutter.dumbo.markdown;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.kohlschutter.stringhold.IOSupplier;
import com.kohlschutter.stringhold.StringHolder;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;

public class LiquidMarkdownSupport {
  private final LiquidSupport liquidSupport;

  // flexmark-java
  private final Parser parser;
  private final HtmlRenderer renderer;

  public LiquidMarkdownSupport(LiquidSupport liquidSupport) {
    this.liquidSupport = liquidSupport;

    // flexmark-java
    this.parser = Parser.builder().build();
    this.renderer = HtmlRenderer.builder().build();
  }

  public Document parse(File mdFile, Map<String, Object> variables) throws FileNotFoundException,
      IOException {

    return parse(() -> new BufferedReader(new InputStreamReader(new FileInputStream(mdFile),
        StandardCharsets.UTF_8)), (int) mdFile.length(), variables);
  }

  public Document parse(IOSupplier<Reader> in, int estimatedLen, Map<String, Object> variables)
      throws IOException {
    StringHolder liquid = liquidSupport.prerender(in, estimatedLen, variables);
    return parser.parseReader(liquid.toReader());
  }

  public String render(Document document) {
    return renderer.render(document);
  }

  public void render(Document document, Appendable appendable) {
    renderer.render(document, appendable);
  }
}
