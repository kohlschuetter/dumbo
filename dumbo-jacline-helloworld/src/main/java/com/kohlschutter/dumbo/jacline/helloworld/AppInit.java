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

import com.kohlschutter.dumbo.js.Dumbo;
import com.kohlschutter.jacline.annotations.JsEntryPoint;
import com.kohlschutter.jacline.lib.log.CommonLog;
import com.kohlschutter.jacline.lib.pledge.Pledge;

import elemental2.dom.DomGlobal;
import jsinterop.base.Js;

@JsEntryPoint
public class AppInit {
  static {
    Dumbo.whenLive(AppInit::whenLive);

    Pledge.<Integer> ofCallback((success) -> {
      success.consume(1);
    }).then((i) -> {
      CommonLog.log("The input: {} — The output: {}", i, i + 23);
      return i + 23;
    }).thenAccept((i) -> {
      CommonLog.log("The result is ", i);
    }).exceptionallyAccept((o) -> {
      DomGlobal.console.log(o);
    });
  }

  @SuppressWarnings("PMD.GuardLogStatement") // false positive
  public static void whenLive() {
    DemoServiceAsync service = Dumbo.getService(DemoServiceAsync.class);

    service.hello$async(false).thenAccept((o) -> {
      Dumbo.setText("#rpcResponse", o);
    }).exceptionally((e) -> {
      DomGlobal.console.error("Hello returned error: " + e); // Catch section is optional
      return Js.cast(e);
    });

    // possible, but discouraged (it's blocking the main thread)
    // Dumbo.setText(service.hello(false));
  }
}
