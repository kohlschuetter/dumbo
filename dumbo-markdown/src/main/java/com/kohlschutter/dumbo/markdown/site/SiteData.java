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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.snakeyaml.engine.v2.api.Load;

import com.kohlschutter.dumbo.ServerApp;
import com.kohlschutter.dumbo.markdown.YAMLSupport;

/**
 * Provides a Jekyll-compatible site.data object.
 *
 * @author Christian Kohlschütter
 */
public class SiteData extends FilterMap.ReadOnlyFilterMap<String, Object> {
  private static final Object NOT_FOUND = new Object();
  private final ServerApp app;
  private final String base;

  SiteData(ServerApp app, String base) {
    super(new HashMap<>());
    this.app = app;
    this.base = base;
  }

  @Override
  public boolean containsKey(Object key) {
    return get(key) != null;
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object get(Object k) {
    String key = k.toString();
    Object value = getMap().get(key);
    if (value != null) {
      if (value == NOT_FOUND) {
        return null;
      }
      return value;
    }

    String path = base + "/" + key;
    if (path.contains("..")) {
      // throw new IllegalArgumentException();
      getMap().put(key, NOT_FOUND);
      return null;
    }
    URL url = app.getResource(path);
    if (url != null) {
      // directory
      value = new SiteData(app, path);
      getMap().put(key, value);
      return value;
    }

    String ymlPath = path + ".yml";
    url = app.getResource(ymlPath);
    if (url != null) {
      InputStream in;
      try {
        in = url.openStream();
      } catch (IOException e) {
        e.printStackTrace();
        in = null;
      }
      if (in == null) {
        getMap().put(key, NOT_FOUND);
        return null;
      }
      try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
        Object obj = new Load(YAMLSupport.DEFAULT_LOAD_SETTINGS).loadFromReader(reader);
        if (obj == null) {
          getMap().put(key, NOT_FOUND);
          return null;
        }
        getMap().put(key, obj);

        return obj;
      } catch (IOException e) {
        e.printStackTrace();
        getMap().put(key, NOT_FOUND);
        return null;
      } finally {
        try {
          in.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    getMap().put(key, NOT_FOUND);
    return null;
  }
}