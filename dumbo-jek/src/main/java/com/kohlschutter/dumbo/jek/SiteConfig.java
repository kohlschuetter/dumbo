/*
 * Copyright 2022 Christian Kohlschütter
 * Copyright 2014,2015 Evernote Corporation.
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
package com.kohlschutter.dumbo.jek;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.snakeyaml.engine.v2.api.Load;

import com.kohlschutter.dumbo.ServerApp;

import jakarta.servlet.ServletException;

public class SiteConfig {

  @SuppressWarnings("unchecked")
  public static Map<? extends String, ? extends Object> init(ServerApp app)
      throws ServletException {
    Map<String, Object> siteVariables = new HashMap<>();

    URL configYml = app.getResource("markdown/_config.yml");
    if (configYml != null) {
      try {
        Object config = new Load(YAMLSupport.DEFAULT_LOAD_SETTINGS).loadFromReader(
            new InputStreamReader(configYml.openStream(), StandardCharsets.UTF_8));
        if (!(config instanceof Map)) {
          throw new ServletException("Could not parse " + configYml + ": not a Map");
        }
        siteVariables.putAll((Map<String, Object>) config);
      } catch (IOException e) {
        throw new ServletException("Could not parse " + configYml);
      }
      System.out.println("Loaded " + configYml);
    } else {
      System.out.println("Not found: " + configYml);
    }

    siteVariables.put("data", new DataObject(app, "markdown/_data"));

    return siteVariables;
  }

  public static class DataObject extends HashMap<Object, Object> {
    private static final long serialVersionUID = 1L;
    private static final Object NOT_FOUND = new Object();
    private final ServerApp app;
    private final String base;

    DataObject(ServerApp app, String base) {
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
      Object value = super.get(key);
      if (value != null) {
        if (value == NOT_FOUND) {
          return null;
        }
        return value;
      }

      String path = base + "/" + key;
      if (path.contains("..")) {
        // throw new IllegalArgumentException();
        super.put(key, NOT_FOUND);
        return null;
      }
      URL url = app.getResource(path);
      if (url != null) {
        // directory
        value = new DataObject(app, path);
        super.put(key, value);
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
          super.put(key, NOT_FOUND);
          return null;
        }
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
          Object obj = new Load(YAMLSupport.DEFAULT_LOAD_SETTINGS).loadFromReader(reader);
          if (obj == null) {
            super.put(key, NOT_FOUND);
            return null;
          }
          super.put(key, obj);

          return obj;
        } catch (IOException e) {
          e.printStackTrace();
          super.put(key, NOT_FOUND);
          return null;
        } finally {
          try {
            in.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }

      super.put(key, NOT_FOUND);
      return null;
    }

    @Override
    public Object put(Object key, Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends Object, ? extends Object> m) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }
}
