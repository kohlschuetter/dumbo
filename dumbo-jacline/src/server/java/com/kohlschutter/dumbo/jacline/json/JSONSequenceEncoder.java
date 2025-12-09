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

import java.util.Arrays;

import org.json.JSONArray;

import com.kohlschutter.jacline.annotations.JsIgnoreType;
import com.kohlschutter.jacline.lib.coding.CodingException;
import com.kohlschutter.jacline.lib.coding.ObjectEncoder;
import com.kohlschutter.jacline.lib.coding.SequenceEncoder;

@JsIgnoreType
public final class JSONSequenceEncoder implements SequenceEncoder {
  private final JSONArray array = new JSONArray();
  private final JSONSequenceEncoder parent;

  public JSONSequenceEncoder() {
    this(null);
  }

  private JSONSequenceEncoder(JSONSequenceEncoder parent) {
    this.parent = parent;
  }

  @Override
  public SequenceEncoder encodeStrings(String... values) {
    array.put(Arrays.asList(values));
    return this;
  }

  @Override
  public SequenceEncoder encodeBooleans(Boolean... values) {
    array.put(Arrays.asList(values));
    return this;
  }

  @Override
  public SequenceEncoder encodeNumbers(Number... values) {
    array.put(Arrays.asList(values));
    return this;
  }

  @Override
  public SequenceEncoder beginEncodeArray() {
    return new JSONSequenceEncoder(this);
  }

  @Override
  public SequenceEncoder encodeObjects(ObjectEncoder encoder, Object... objs)
      throws CodingException {

    try (JSONKeyEncoder enc = new JSONKeyEncoder(null)) {
      encoder.encode(enc);
      array.put(enc.getEncoded());
    }

    return this;
  }

  @Override
  public SequenceEncoder end() {
    JSONSequenceEncoder p = this.parent;
    if (p != null) {
      p.array.put(this.array);
      return p;
    } else {
      return this;
    }
  }

  @Override
  public JSONArray getEncoded() {
    return array;
  }
}
