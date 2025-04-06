package com.kohlschutter.dumbo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServletInitParameter {
  String key();

  String value();

  /**
   * Specify how the value is provided.
   * <p>
   * If it's {@code String.class} (the default), the value specified as {@link #value()} is taken
   * as-is.
   * <p>
   * If it's another class, then a static method with the name specified in {@link #value()} is
   * called to obtain the actual value. If the type is of a known scoped-singleton instance, e.g.,
   * {@code ServerApp}, the method name may also refer to an instance method.
   * <p>
   * By contract, the method must be idempotent. Moreover, there is no guarantee as to when the
   * method is called.
   * 
   * @return The provider class, or {@code String.class} (the default) for "string as-is".
   */
  Class<?> valueProvider() default String.class;
}
