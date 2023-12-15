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
package com.kohlschutter.dumbo.service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboServer;

/**
 * A {@link DumboServerService} for the Java Service Provider Interface (SPI), which allows sharing
 * a single service implementation for multiple SPI service implementations.
 *
 * @author Christian Kohlschütter
 */
public abstract class SPIDumboServerService implements DumboServerService {
  private static final Map<Class<?>, DumboServerService> MAP = new HashMap<>();
  private final DumboServerService service;

  protected SPIDumboServerService(Class<? extends DumboApplication> applicationClass,
      DumboServerServiceSupplier implSupplier) throws InterruptedException, IOException {
    service = registerService(applicationClass, implSupplier);
  }

  @FunctionalInterface
  public interface DumboServerServiceSupplier {
    DumboServerService newInstance(Class<? extends DumboApplication> applicationClass)
        throws IOException, InterruptedException;
  }

  public static DumboServerService registerService(
      Class<? extends DumboApplication> applicationClass, DumboServerServiceSupplier implSupplier)
      throws InterruptedException, IOException {
    DumboServerService existingService = MAP.get(applicationClass);
    if (existingService != null) {
      return existingService;
    }
    DumboServerService service = implSupplier.newInstance(applicationClass);
    MAP.put(applicationClass, service);
    return service;
  }

  @Override
  public <T> Collection<T> getDumboServices(Class<T> clazz) {
    return service.getDumboServices(clazz);
  }

  @Override
  public DumboServer getDumboServer() {
    return service.getDumboServer();
  }
}
