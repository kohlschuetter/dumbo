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

import com.kohlschutter.dumbo.RenderState;
import com.kohlschutter.dumbo.ext.prism.PrismSupport;
import com.kohlschutter.dumbo.markdown.flexmark.CustomFencedCodeRenderer;
import com.kohlschutter.stringhold.StringHolder;
import com.kohlschutter.stringhold.StringHolderSequence;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.Parser.Builder;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.ast.Document;

/**
 * Markdown support, using flexmark-java.
 *
 * @author Christian Kohlschütter
 */
public class MarkdownHelper {
  private final Parser markdownParser;
  private final HtmlRenderer htmlRenderer;

  public MarkdownHelper() {
    AttributesExtension attributesExtension = AttributesExtension.create();

    Builder parserBuilder = Parser.builder();
    parserBuilder.setFrom(ParserEmulationProfile.KRAMDOWN);
    parserBuilder.set(AttributesExtension.FENCED_CODE_INFO_ATTRIBUTES, true);

    attributesExtension.extend(parserBuilder);
    this.markdownParser = parserBuilder.build();

    com.vladsch.flexmark.html.HtmlRenderer.Builder htmlBuilder = HtmlRenderer.builder();
    htmlBuilder.nodeRendererFactory(new CustomFencedCodeRenderer.Factory());
    this.htmlRenderer = htmlBuilder.build();
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
   * Renders the parsed Markdown document to an {@link Appendable}.
   *
   * @param document The document.
   */
  public void render(Document document, StringHolderSequence appendable) {
    htmlRenderer.render(document, appendable);

    if (document.contains(Parser.FENCED_CODE_CONTENT_BLOCK)) {
      RenderState.get().setMarkedUsed(PrismSupport.class);
    }
  }
}
