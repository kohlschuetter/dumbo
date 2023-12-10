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
