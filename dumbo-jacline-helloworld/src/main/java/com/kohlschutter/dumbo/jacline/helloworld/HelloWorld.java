/*
 * dumbo-jacline-helloworld
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
package com.kohlschutter.dumbo.jacline.helloworld;

import java.io.IOException;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;
import com.kohlschutter.jacline.annotations.JsEntryPoint;
import com.kohlschutter.jacline.annotations.JsExport;
import com.kohlschutter.jacline.lib.coding.Codable;
import com.kohlschutter.jacline.lib.coding.CodingException;
import com.kohlschutter.jacline.lib.coding.CodingProviders;
import com.kohlschutter.jacline.lib.coding.Decodables;
import com.kohlschutter.jacline.lib.coding.KeyDecoder;
import com.kohlschutter.jacline.lib.coding.KeyDecoderProvider;
import com.kohlschutter.jacline.lib.coding.KeyEncoder;
import com.kohlschutter.jacline.lib.coding.KeyEncoderProvider;
import com.kohlschutter.jacline.lib.coding.StandardArrayDecoders;
import com.kohlschutter.jacline.lib.coding.StandardArrayEncoders;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * A simple hello world example.
 */
@JsType
@JsExport
@JsEntryPoint
public class HelloWorld implements Codable {
  private static final String CODED_TYPE = "HelloWorld";
  private String message = "Hello World!!!";
  private Object[] array;

  static {
    Decodables.setDecoder(CODED_TYPE, HelloWorld::decode);
  }

  @JsExport
  public static String getHelloWorld() {
    return "Hello from Java!";
  }

  @JsExport
  @JsProperty
  @SuppressWarnings("PMD.MethodReturnsInternalArray")
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Object[] getArray() {
    return array;
  }

  @JsExport
  @JsProperty(name = "message")
  public String getMessage() {
    return message;
  }

  private void setMessage(String m) {
    this.message = m;
  }

  @JsExport
  public static HelloWorld decode(KeyDecoderProvider provider, Object obj) throws CodingException {
    try (KeyDecoder jl = CodingProviders.decorateDecoderProvider(provider).load(CODED_TYPE, obj)) {
      HelloWorld hw = new HelloWorld();
      hw.setMessage(jl.stringForKey("message"));

      hw.array = jl.arrayForKey("arr", StandardArrayDecoders::strings);

      return hw;
    } catch (IOException e) {
      throw new CodingException(e);
    }
  }

  @Override
  @JsExport
  @SuppressFBWarnings("CNT_ROUGH_CONSTANT_VALUE")
  public Object encode(KeyEncoderProvider provider) throws CodingException {
    KeyEncoder enc = CodingProviders.decorateEncoderProvider(provider).begin(CODED_TYPE);
    enc.encodeString("message", message);
    enc.beginEncodeObject("obj", "someObj").encodeBoolean("cool", true).encodeNumber("pi", 3.14)
        .end();
    enc.encodeArray("arr", StandardArrayEncoders::strings, array);
    return enc.getEncoded();
  }
}
