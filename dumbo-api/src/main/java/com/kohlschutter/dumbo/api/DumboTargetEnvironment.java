/*
 * Copyright 2022,2023 Christian Kohlschütter
 * Copyright 2014,2015 Evernote Corporation.
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
package com.kohlschutter.dumbo.api;

public enum DumboTargetEnvironment {
  DEVELOPMENT(DumboServerBuilderConfigurator::noop), //
  PRODUCTION(DumboServerBuilderConfigurator::noop), //
  CONTAINER(DumboTargetEnvironment::configureForContainer), //
  APPENGINE(DumboTargetEnvironment::configureForContainer); //

  private final DumboServerBuilderConfigurator builderConfigurator;

  DumboTargetEnvironment(DumboServerBuilderConfigurator builderConfigurator) {
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
