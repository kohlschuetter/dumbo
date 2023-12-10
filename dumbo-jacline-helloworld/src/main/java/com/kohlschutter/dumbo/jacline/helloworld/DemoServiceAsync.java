package com.kohlschutter.dumbo.jacline.helloworld;

import com.kohlschutter.dumbo.annotations.DumboService;
import com.kohlschutter.jacline.lib.pledge.Pledge;

/**
 * jsweet stub to expose async-variants of {@link DemoService} methods.
 * 
 * NOTE: The classname must be equal to the classname being extended, plus the suffix {@code Async}.
 * This is detected automatically on the JavaScript side. Async methods should be declared as
 * methods with a {@code default} implementation returning null, and named as the non-async method
 * name, plus the suffix {@code $async}. The return type must be a {@code Promise} of the non-async
 * return type. Other than that they should be identical in signature to the non-async variants.
 * 
 * @author Christian Kohlschütter
 */
@SuppressWarnings({
    "MethodName", // Checkstyle
    "PMD.AvoidDollarSigns", "PMD.MethodNamingConventions", // PMD
})
@DumboService
public interface DemoServiceAsync extends DemoService {
  Pledge<String> hello$async(boolean error);

  Pledge<Float> echoFloat$async(float f);

  Pledge<Object> echoObject$async(Object obj);
}
