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
package com.kohlschutter.dumbo.markdown.flexmark;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ext.attributes.AttributeNode;
import com.vladsch.flexmark.ext.attributes.AttributesNode;
import com.vladsch.flexmark.html.HtmlRendererOptions;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

/**
 * The node renderer that renders all the core nodes (comes last in the order of node renderers).
 */
public class CustomFencedCodeRenderer implements NodeRenderer {
  final public static AttributablePart LOOSE_LIST_ITEM = new AttributablePart("LOOSE_LIST_ITEM");
  final public static AttributablePart TIGHT_LIST_ITEM = new AttributablePart("TIGHT_LIST_ITEM");
  final public static AttributablePart PARAGRAPH_LINE = new AttributablePart("PARAGRAPH_LINE");
  final public static AttributablePart CODE_CONTENT = new AttributablePart("FENCED_CODE_CONTENT");

  final private boolean codeContentBlock;

  public CustomFencedCodeRenderer(DataHolder options) {
    codeContentBlock = Parser.FENCED_CODE_CONTENT_BLOCK.get(options);
  }

  @Override
  public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
    return new HashSet<>(Arrays.asList(new NodeRenderingHandler<>(FencedCodeBlock.class,
        this::render)));
  }

  void render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
    html.line();

    html.srcPosWithTrailingEOL(node.getChars()).withAttr();

    AttributesNode attributesNode = (AttributesNode) node.getFirstChildAny(AttributesNode.class);

    String file = null;
    if (attributesNode != null) {
      for (Node n : attributesNode.getChildren()) {
        if (n instanceof AttributeNode) {
          AttributeNode an = (AttributeNode) n;
          if ("file".equals(an.getName().normalizeEOL())) {
            file = an.getValue().normalizeEOL();
          }
        }
      }
    }
    boolean useRouge = (file != null);

    if (useRouge) {
      html.attr("class", "highlighter-rouge");
      // html.attr("style", "padding-left: 2em");
    }
    html.tag("div");

    if (useRouge && file != null) {
      html.withAttr().attr("class", "code-header").tag("div");

      html.withAttr().attr("data-label-text", file).tag("span");
      html.withAttr().attr("class", "far fa-file-code").tag("i").tag("/i");
      html.tag("/span");
      html.withAttr().attr("aria-label", "copy").attr("data-title-succeed", "Copied!").tag(
          "button");
      // html.withAttr().attr("class", "far fa-clipboard").tag("i").tag("/i");
      html.tag("/button");

      html.tag("/div");

      html.withAttr().attr("class", "highlight").tag("div");
    }

    BasedSequence info = node.getInfo();
    HtmlRendererOptions htmlOptions = context.getHtmlOptions();
    if (info.isNotNull() && !info.isBlank()) {
      String language = node.getInfoDelimitedByAny(htmlOptions.languageDelimiterSet).unescape();
      String languageClass = htmlOptions.languageClassMap.getOrDefault(language,
          htmlOptions.languageClassPrefix + language);
      html.attr("class", languageClass);
    } else {
      String noLanguageClass = htmlOptions.noLanguageClass.trim();
      if (!noLanguageClass.isEmpty()) {
        html.attr("class", noLanguageClass);
      }
    }

    html.tag("pre").openPre();

    html.srcPosWithEOL(node.getContentChars()).withAttr(CODE_CONTENT).tag("code");
    if (codeContentBlock) {
      context.renderChildren(node);
    } else {
      html.text(node.getContentChars().normalizeEOL());
    }

    html.tag("/code");
    html.tag("/pre").closePre();

    if (useRouge && file != null) {
      html.tag("/div");
    }

    html.tag("/div");
    html.lineIf(htmlOptions.htmlBlockCloseTagEol);
  }

  public static class Factory implements NodeRendererFactory {
    @NotNull
    @Override
    public NodeRenderer apply(@NotNull DataHolder options) {
      return new CustomFencedCodeRenderer(options);
    }
  }
}
