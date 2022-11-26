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
package com.kohlschutter.dumbo.markdown.site;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.snakeyaml.engine.v2.api.Load;

import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.dumbo.markdown.LiquidHelper;
import com.kohlschutter.dumbo.markdown.ResourcePathTraverser;
import com.kohlschutter.dumbo.markdown.YAMLSupport;
import com.kohlschutter.stringhold.IOReaderSupplier;
import com.kohlschutter.stringhold.IOSupplier;

import jakarta.servlet.ServletException;

/**
 * Provides a Jekyll-compatible "site" object.
 *
 * @author Christian Kohlschütter
 */
public final class SiteObject extends FilterMap.ReadOnlyFilterMap<String, Object> {
  @SuppressWarnings("unchecked")
  public SiteObject(ServerApp app, LiquidHelper liquid) throws ServletException {
    super(new HashMap<>());
    URL configYml = app.getResource("markdown/_config.yml");
    if (configYml != null) {
      try {
        Object config = new Load(YAMLSupport.DEFAULT_LOAD_SETTINGS).loadFromReader(
            new InputStreamReader(configYml.openStream(), StandardCharsets.UTF_8));
        if (!(config instanceof Map)) {
          throw new ServletException("Could not parse " + configYml + ": not a Map");
        }
        getMap().putAll((Map<String, Object>) config);
      } catch (IOException e) {
        throw new ServletException("Could not parse " + configYml);
      }
      System.out.println("Loaded " + configYml);
    } else {
      System.out.println("Not found: " + configYml);
    }

    getMap().put("data", new SiteData(app, "markdown/_data"));

    getMap().put("posts", new SiteCollection(liquid, new HashMap<>(), getCollection(app, "posts")));
    getMap().put("tabs", new SiteCollection(liquid, new HashMap<>(), getCollection(app, "tabs")));
    getMap().put("drafts", new SiteCollection(liquid, new HashMap<>(), getCollection(app,
        "drafts")));
  }

  private static List<IOSupplier<Reader>> getCollection(ServerApp app, String collectionId)
      throws ServletException {
    Collection<URL> urls;
    try {
      urls = ResourcePathTraverser.findURLs(app.getClass(), "markdown/_" + collectionId, false, (
          n) -> n.endsWith(".md")).values();
    } catch (IOException e) {
      throw new ServletException(e);
    }

    return urls.stream().map((u) -> IOReaderSupplier.withContentsOf(u, StandardCharsets.UTF_8))
        .collect(Collectors.toList());
  }
}
