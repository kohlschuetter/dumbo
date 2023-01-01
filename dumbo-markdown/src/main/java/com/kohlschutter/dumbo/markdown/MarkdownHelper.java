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

import java.io.IOException;
import java.io.Reader;

import com.kohlschutter.stringhold.StringHolder;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;

public class MarkdownHelper {
  // flexmark-java
  private final Parser markdownParser;
  private final HtmlRenderer htmlRenderer;

  public MarkdownHelper() {
    // flexmark-java
    this.markdownParser = Parser.builder().build();
    this.htmlRenderer = HtmlRenderer.builder().build();
  }

  public Document parseMarkdown(Object obj) throws IOException {
    if (obj instanceof StringHolder) {
      StringHolder sh = (StringHolder) obj;
      if (sh.isString()) {
        return markdownParser.parseReader(sh.toReader());
      } else {
        return markdownParser.parse(sh.toString());
      }
    } else if (obj instanceof Reader) {
      return markdownParser.parseReader((Reader) obj);
    } else {
      return markdownParser.parse(String.valueOf(obj));
    }
  }

  /**
   * Renders the parsed Markdown document to string.
   *
   * @param document The document.
   * @return The rendered document as a string.
   */
  public String render(Document document) {
    return htmlRenderer.render(document);
  }

  /**
   * Renders the parsed Markdown document to an {@link Appendable}.
   *
   * @param document The document.
   */
  public void render(Document document, Appendable appendable) {
    htmlRenderer.render(document, appendable);
  }

}
