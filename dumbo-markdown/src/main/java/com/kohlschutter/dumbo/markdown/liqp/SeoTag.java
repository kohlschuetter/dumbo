/*
 * dumbo-markdown
 *
 * Copyright 2022 Christian Kohlschütter
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

import liqp.TemplateContext;
import liqp.nodes.LNode;
import liqp.tags.Tag;

public class SeoTag extends Tag {

  public SeoTag() {
    super("seo");
  }

  @Override
  public Object render(TemplateContext context, LNode... nodes) {

    System.out.println("FIXME: SEO TAG, nodes: " + nodes.length);

    return "";
  }
}
