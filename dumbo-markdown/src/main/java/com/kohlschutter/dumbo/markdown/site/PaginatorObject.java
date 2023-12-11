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
package com.kohlschutter.dumbo.markdown.site;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kohlschutter.dumbo.markdown.LiquidVariables;

/**
 * Provides a Jekyll-compatible paginator object.
 *
 * @author Christian Kohlschütter
 *
 *
 */
// FIXME not fully implemented yet
public class PaginatorObject extends FilterMap.ReadOnlyFilterMap<String, Object> {
  private final List<Object> posts; // NOPMD.SingularField

  @SuppressWarnings("unchecked")
  public PaginatorObject(Map<String, Object> commonVariables) {
    super(new HashMap<>());

    this.posts = (List<Object>) ((Map<String, Object>) commonVariables.get(LiquidVariables.SITE))
        .get(LiquidVariables.SITE_POSTS);

    Map<String, Object> map = getMap();
    int perPage = 5;
    int totalPosts = posts.size();
    int totalPages = (int) Math.ceil(totalPosts / (float) perPage);
    map.put("page", 1);
    map.put("per_page", perPage);
    map.put("posts", posts);
    map.put("total_posts", totalPosts);
    map.put("total_pages", totalPages);
    map.put("previous_page", null);
    map.put("previous_page_path", null);
    map.put("next_page", null);
    map.put("next_page_path", null);
  }
}
