package com.kohlschutter.dumbo.api;

@FunctionalInterface
public interface DumboServerBuilderConfigurator {
  DumboServerBuilder configure(DumboServerBuilder builer);

  static DumboServerBuilder noop(DumboServerBuilder builder) {
    return builder;
  }
}
