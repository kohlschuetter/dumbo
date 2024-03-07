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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.kohlschutter.dumbo.api.DumboServiceProviders;

class DemoServiceTest {

  @ParameterizedTest
  @MethodSource("demoServices")
  void testEchoFloat(DemoService service) {
    assertEquals(12.34f, service.echoFloat(12.34f));
  }

  @ParameterizedTest
  @MethodSource("demoServices")
  void testEchoObject(DemoService service) {
    Map<String, Object> jobj = new HashMap<String, Object>();
    jobj.put("foo", "bar");
    jobj.put("baz", 123);

    assertEquals(jobj, service.echoObject(jobj));
  }

  @ParameterizedTest
  @MethodSource("demoServices")
  void testHello(DemoService service) throws Exception {
    assertEquals("world", service.hello(false));
  }

  @ParameterizedTest
  @MethodSource("demoServices")
  void testHelloException(DemoService service) {
    assertThrows(Exception.class, () -> service.hello(true));
  }

  @SuppressWarnings("null")
  static Stream<DemoService> demoServices() {
    return DumboServiceProviders.allRegisteredImplementationsForService(DemoService.class);
  }
}
