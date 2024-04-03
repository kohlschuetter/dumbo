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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;
import com.kohlschutter.dumbo.DumboServerImpl;
import com.kohlschutter.dumbo.DumboServerImplBuilder;
import com.kohlschutter.dumbo.JsonRpcClient;
import com.kohlschutter.dumbo.annotations.DumboService;
import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboServer;

public class DumboServerServiceImpl implements DumboServerService {
  private final DumboServerImpl server;
  private final JsonRpcClient client;
  private final Set<String> knownRpcServices;

  @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
  @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
  public DumboServerServiceImpl(Class<? extends DumboApplication> applicationClass)
      throws IOException, InterruptedException {
    server = newServerImpl(applicationClass).start();

    this.client = server.newJsonRpcClient();
    this.knownRpcServices = retrieveKnownRpcServices();
  }

  private Set<String> retrieveKnownRpcServices() {
    Set<String> services = new HashSet<>();
    for (String m : client.retrieveKnownRpcMethods()) {
      int lastDot = m.lastIndexOf('.');
      if (lastDot == -1) {
        continue;
      }
      services.add(m.substring(0, lastDot));
    }
    return services;
  }

  protected <T> T decorateRpcClientProxy(T proxy) {
    return proxy;
  }

  @Override
  public <T> Collection<T> getDumboServices(Class<T> clazz) {
    DumboService ds = clazz.getAnnotation(DumboService.class);
    String rpcName = ds.rpcName();
    if (rpcName == null || rpcName.isEmpty()) {
      rpcName = clazz.getName();
    }

    return Stream.of(server.getDumboService(clazz), knownRpcServices.contains(rpcName)
        ? decorateRpcClientProxy(client.openProxy(rpcName, clazz)) : null).filter((p) -> p != null)
        .toList();
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  @Override
  public DumboServer getDumboServer() {
    return server;
  }

  protected DumboServerImpl getDumboServerImpl() {
    return server;
  }

  private DumboServerImpl newServerImpl(Class<? extends DumboApplication> applicationClass)
      throws IOException {
    DumboServerImplBuilder builder = new DumboServerImplBuilder();
    configureNewServerImpl(builder);
    builder.withApplication(applicationClass);
    return (DumboServerImpl) builder.build();
  }

  protected void configureNewServerImpl(DumboServerImplBuilder builder) {
  }
}
