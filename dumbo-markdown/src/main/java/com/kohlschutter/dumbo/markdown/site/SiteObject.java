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
import com.kohlschutter.dumbo.markdown.YAMLSupport;
import com.kohlschutter.dumbo.markdown.util.PathReaderSupplier;
import com.kohlschutter.util.ResourcePathTraverser;

/**
 * Provides a Jekyll-compatible "site" object.
 *
 * @author Christian Kohlschütter
 */
public final class SiteObject extends FilterMap.ReadOnlyFilterMap<String, Object> {

  private final ServerApp app;
  private final LiquidHelper liquid;

  @SuppressWarnings("unchecked")
  private static Map<String, Object> mergeMaps(Map<String, Object> target,
      Map<String, Object> newValues) {
    newValues.forEach((k, newVal) -> {

      if (newVal == null) {
        return;
      }

      target.merge(k, newVal, (a, b) -> {
        if (a instanceof Map && b instanceof Map) {
          return mergeMaps((Map<String, Object>) a, (Map<String, Object>) b);
        } else if (b != null) {
          return b;
        } else {
          return a;
        }
      });
    });

    return target;
  }

  private void mergeConfig(URL configYml) {
    Map<String, Object> map = getMap();
    try {
      Object config = new Load(YAMLSupport.DEFAULT_LOAD_SETTINGS).loadFromReader(
          new InputStreamReader(configYml.openStream(), StandardCharsets.UTF_8));
      if (!(config instanceof Map)) {
        throw new IllegalStateException("Could not parse " + configYml + ": not a Map");
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> configMap = (Map<String, Object>) config;
      mergeMaps(map, configMap);
    } catch (IOException e) {
      throw new IllegalStateException("Could not parse " + configYml);
    }
  }

  public SiteObject(ServerApp app, LiquidHelper liquid) {
    super(new HashMap<>());
    this.app = app;
    this.liquid = liquid;

    Map<String, Object> map = getMap();

    // Load default config
    URL defaultConfigYml = getClass().getResource("defaultConfig.yml");
    if (defaultConfigYml == null) {
      throw new IllegalStateException("defaultConfig.yml not found");
    }
    mergeConfig(defaultConfigYml);

    // Load site-specific config
    URL configYml = app.getResource("markdown/_config.yml");
    if (configYml != null) {
      System.out.println("Load " + configYml);
      mergeConfig(configYml);
    } else {
      System.out.println("Not found: markdown/_config.yml");
    }

    map.put("data", new SiteData(app, "markdown/_data"));
  }

  public void initCollections() {
    Map<String, Object> map = getMap();

    @SuppressWarnings("unchecked")
    final Map<String, Object> collectionsConfig = (Map<String, Object>) map.get("collections");

    for (String collectionId : collectionsConfig.keySet()) {
      map.put(collectionId, new SiteCollection(liquid, collectionsConfig, collectionId,
          new HashMap<>(), getCollection(app, collectionId)));
    }
  }

  private static List<PathReaderSupplier> getCollection(ServerApp app, String collectionId) {
    Collection<URL> urls;

    String baseUrl = app.getApplicationClass().getResource("markdown/").toString();
    try {
      urls = ResourcePathTraverser.findURLs(app.getApplicationClass(), "markdown/_" + collectionId,
          false, ResourcePathTraverser.Order.ALPHABETICALLY_REVERSE, (n) -> n.endsWith(".md"))
          .values();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return urls.stream().map((u) -> {
      String uString = u.toString();
      if (!uString.startsWith(baseUrl)) {
        throw new IllegalStateException("URL does not start with " + baseUrl + ": " + uString);
      }
      String relativeUrl = uString.substring(baseUrl.length());

      return PathReaderSupplier.withContentsOf(collectionId, relativeUrl, u,
          StandardCharsets.UTF_8);
    }).collect(Collectors.toList());
  }
}
