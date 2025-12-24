/*
 * dumbo-jacline
 *
 * Copyright 2023 Christian Kohlschütter
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
package com.kohlschutter.dumbo.js;

import java.util.Map;

import com.kohlschutter.jacline.annotations.JsEntryPoint;
import com.kohlschutter.jacline.lib.coding.Codable;
import com.kohlschutter.jacline.lib.coding.CodingException;
import com.kohlschutter.jacline.lib.coding.CodingServiceProvider;
import com.kohlschutter.jacline.lib.coding.KeyEncoder;
import com.kohlschutter.jacline.lib.log.CommonLog;
import com.kohlschutter.jacline.lib.util.JaclineUtil;

@JsEntryPoint
final class JaclineInit {
  static {
    if (JaclineUtil.isJavaScriptEnvironment()) {
      Dumbo.registerMarshallFiltersForJacline();
    }
  }

  private static final CodingServiceProvider CSP = CodingServiceProvider.getDefault();

  @SuppressWarnings("PMD.CognitiveComplexity")
  static Object preMarshallObject(Object obj) {
    if (obj instanceof Codable) {
      try {
        obj = ((Codable) obj).encode(CSP);
      } catch (CodingException e) {
        CommonLog.error("Could not encode object for Jacline: CodingException", e);
        throw new IllegalStateException(e);
      }
    } else if (obj instanceof Map) {
      try (KeyEncoder kenc = CSP.keyEncoder("java.util.HashMap").beginEncodeObject("map", null)) {
        for (Map.Entry<?, ?> en : ((Map<?, ?>) obj).entrySet()) {
          String key = String.valueOf(en.getKey());
          Object val = preMarshallObject(en.getValue());

          if (val == null) {
            kenc.encodeString(key, null);
          } else if (val instanceof Number) {
            kenc.encodeNumber(key, (Number) val);
          } else if (val instanceof String) {
            kenc.encodeString(key, (String) val);
          } else if (val instanceof Boolean) {
            kenc.encodeNumber(key, ((Boolean) val) ? 1 : 0);
          } else {
            CommonLog.error("Could not encode object for Jacline; unsupported value", val);
            throw new IllegalStateException("Unsupported value");
          }
        }
        obj = kenc.end().end().getEncoded();
      } catch (RuntimeException e) { // NOPMD
        CommonLog.error("Could not encode object for Jacline", e);
        throw e;
      } catch (Exception e) { // NOPMD
        CommonLog.error("Could not encode object for Jacline", e);
        throw new IllegalStateException(e);
      }
    }
    return obj;
  }
}
