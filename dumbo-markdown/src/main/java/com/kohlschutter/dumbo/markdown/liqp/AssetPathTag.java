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

import java.net.URI;
import java.util.Map;

import liqp.TemplateContext;
import liqp.nodes.LNode;
import liqp.tags.Tag;

public class AssetPathTag extends Tag {

  public AssetPathTag() {
    super("asset_path");
  }

  @Override
  public Object render(TemplateContext context, LNode... nodes) {
    if (nodes.length == 1) {
      String v = asString(nodes[0].render(context), context);

      URI u = URI.create(v);
      if (u.isAbsolute()) {
        return u;
      } else {
        URI pageUrl = URI.create("/assets/" + getPageUrl(context));
        u = pageUrl.resolve(u);
        return u;
      }
    } else {
      throw new UnsupportedOperationException("Support for secondary argument not implemented");
    }
  }

  private String getPageUrl(TemplateContext context) {
    Object pageObj = context.get("page");
    if (!(pageObj instanceof Map)) {
      return null;
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> pageMap = (Map<String, Object>) pageObj;
    Object urlObj = pageMap.get("url");
    if (urlObj == null) {
      return null;
    }
    return String.valueOf(urlObj);
  }
}
