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

import liqp.TemplateContext;
import liqp.nodes.LNode;
import liqp.tags.Tag;

public class AssetPathTag extends Tag {

  public AssetPathTag() {
    super("asset_path");
  }

  @Override
  public Object render(TemplateContext context, LNode... nodes) {

    System.out.println("FIXME: ASSET PATH, nodes: " + nodes.length);
    if (nodes.length == 1) {
      String v = asString(nodes[0].render(context), context);
      System.out.println("ASSET PATH node[0]: " + v);
      return "/assets/posts/2022/10/28/linux-nanopi-r4s/" + v;
    }

    return "";
  }
}
