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
    Object liquid = liquidSupport.prerender(in, estimatedLen, variables);
    if(liquid instanceof StringHolder) {
      return parser.parseReader(((StringHolder)liquid).toReader());
    } else {
      return parser.parse(liquid.toString());
    }
  }

  public String render(Document document) {
    return renderer.render(document);
  }

  public void render(Document document, Appendable appendable) {
    renderer.render(document, appendable);
  }
}
