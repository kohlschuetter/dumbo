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
package com.kohlschutter.dumbo.markdown;

import java.util.Collection;
import java.util.Map;

import org.snakeyaml.engine.v2.api.LoadSettings;

public final class YAMLSupport {

  public static final LoadSettings DEFAULT_LOAD_SETTINGS = LoadSettings.builder()
      .setAllowDuplicateKeys(true).build();

  public static String getVariableAsString(Map<String, Object> variables, String... pathElements) {
    Object obj = variables;
    for (String pathElement : pathElements) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) obj;
      obj = map.get(pathElement);
      if (obj == null) {
        return null;
      }
    }
    boolean loop;
    do {
      loop = false;
      if (obj instanceof Collection) {
        obj = ((Collection<?>) obj).iterator().next();
        loop = true;
      }
      if (obj instanceof Map) {
        obj = ((Map<?, ?>) obj).values();
        loop = true;
      }
      if (obj == null) {
        return null;
      }
    } while (loop);
    return obj.toString();
  }
}
