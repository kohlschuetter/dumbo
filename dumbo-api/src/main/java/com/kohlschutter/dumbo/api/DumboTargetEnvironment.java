package com.kohlschutter.dumbo.api;

public enum DumboTargetEnvironment {
  DEVELOPMENT(DumboServerBuilderConfigurator::noop), //
  PRODUCTION(DumboServerBuilderConfigurator::noop), //
  CONTAINER(DumboTargetEnvironment::configureForContainer), //
  APPENGINE(DumboTargetEnvironment::configureForContainer); //

  private final DumboServerBuilderConfigurator builderConfigurator;

  private DumboTargetEnvironment(DumboServerBuilderConfigurator builderConfigurator) {
    this.builderConfigurator = builderConfigurator;
  }

  public DumboServerBuilder configureBuilder(DumboServerBuilder builder) {
    return builderConfigurator.configure(builder);
  }

  public static DumboTargetEnvironment fromIdentifier(String s) {
    try {
      return valueOf(s);
    } catch (IllegalArgumentException e) {
      for (DumboTargetEnvironment v : values()) {
        if (v.name().equalsIgnoreCase(s)) {
          return v;
        }
      }
      throw e;
    }
  }

  private static DumboServerBuilder configureForContainer(DumboServerBuilder builder) {
    return builder.withPort(8080).withSocketPath(null).withBindAddress(null);
  }
}
