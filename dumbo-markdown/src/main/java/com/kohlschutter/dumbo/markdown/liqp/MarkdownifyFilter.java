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
package com.kohlschutter.dumbo.markdown.liqp;

import java.io.IOException;

import com.kohlschutter.dumbo.markdown.MarkdownHelper;
import com.kohlschutter.stringhold.StringHolder;
import com.kohlschutter.stringhold.StringHolderSequence;

import liqp.TemplateContext;
import liqp.filters.Filter;

public class MarkdownifyFilter extends Filter {
  private final MarkdownHelper mh;

  public MarkdownifyFilter(MarkdownHelper mh) {
    super("markdownify");
    this.mh = mh;
  }

  @Override
  public Object apply(Object value, TemplateContext context, Object... params) {
    if (value == null) {
      // FIXME this is probably a bug
      // throw new NullPointerException("refusing to markdownify null");
      System.err.println("markdownify got null value");
      value = "";
    }

    value = value instanceof StringHolder ? ((StringHolder) value).asContent() : super.asString(
        value, context);
    if (value instanceof CharSequence && ((CharSequence) value).isEmpty()) {
      return "";
    }

    try {
      StringHolderSequence sh = new StringHolderSequence();
      mh.render(mh.parseMarkdown(value), sh);
      return sh;
    } catch (IOException e) {
      throw new RuntimeException("Cannot markdownify content");
    }
  }
}
