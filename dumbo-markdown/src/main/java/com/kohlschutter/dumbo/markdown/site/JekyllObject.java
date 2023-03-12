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
import java.util.Map;

/**
 * Provides a Jekyll-compatible jekyll object.
 *
 * @author Christian Kohlschütter
 */
public class JekyllObject extends FilterMap.ReadOnlyFilterMap<String, Object> {
  public JekyllObject() {
    super(new HashMap<>());

    Map<String, Object> map = getMap();

    // FIXME
    map.put("environment", "development");
    map.put("version", "Dumbo-1.0");
  }
}
