/*
 * jacline-lib-common
 *
 * Copyright 2023 Christian Kohlschütter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kohlschutter.dumbo.jacline.json;

import java.util.Collection;
import java.util.Objects;

import org.json.JSONObject;

import com.kohlschutter.jacline.annotations.JsIgnoreType;
import com.kohlschutter.jacline.lib.coding.ArrayEncoder;
import com.kohlschutter.jacline.lib.coding.CodingAdvisory;
import com.kohlschutter.jacline.lib.coding.CodingException;
import com.kohlschutter.jacline.lib.coding.KeyEncoder;
import com.kohlschutter.jacline.lib.coding.SequenceEncoder;

@JsIgnoreType
public final class JSONKeyEncoder implements KeyEncoder {
  private final JSONKeyEncoder parent;
  private final String parentKey;
  private final JSONObject object = new JSONObject();

  public JSONKeyEncoder(String type) {
    this(type, null, null);
  }

  private JSONKeyEncoder(String type, JSONKeyEncoder parent, String parentKey) {
    this.parent = parent;
    this.parentKey = parentKey;
    if (parent != null) {
      Objects.requireNonNull(parentKey);
    }
    if (type != null) {
      encodeString("javaClass", type);
    }
  }

  @Override
  public KeyEncoder encodeString(String key, String value) {
    object.put(key, value);
    return this;
  }

  @Override
  public KeyEncoder encodeBoolean(String key, Boolean value) {
    object.put(key, value);
    return this;
  }

  @Override
  public KeyEncoder encodeNumber(String key, Number value) {
    object.put(key, value);
    return this;
  }

  @Override
  public KeyEncoder encodeArray(String key, ArrayEncoder encoder, Object[] array)
      throws CodingException {
    if (array == null) {
      object.put(key, (Collection<?>) null);
    } else {
      Object encoded = encoder.encode(array);
      object.put(key, encoded);
    }
    return this;
  }

  @Override
  public KeyEncoder beginEncodeObject(String key, String type) {
    return new JSONKeyEncoder(type, this, key);
  }

  @Override
  public KeyEncoder end() {
    JSONKeyEncoder p = this.parent;
    if (p != null) {
      p.object.put(parentKey, this.object);
      return p;
    } else {
      return this;
    }
  }

  @Override
  public JSONObject getEncoded() {
    return object;
  }

  @Override
  public void markAdvisory(CodingAdvisory advisory) throws CodingException {
    // FIXME
  }

  @Override
  public SequenceEncoder sequenceEncoder() {
    return new JSONSequenceEncoder();
  }

  @Override
  public KeyEncoder keyEncoder(String type) throws CodingException {
    return new JSONKeyEncoder(type);
  }
}
