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
import java.util.UUID;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;
import com.kohlschutter.dumbo.DumboServerImpl;
import com.kohlschutter.dumbo.DumboServerImplBuilder;
import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumborb.client.CustomHeaderURLConnectionSession;
import com.kohlschutter.dumborb.client.Session;
import com.kohlschutter.dumborb.client.SessionAccess;

/**
 * A {@link DumboServerService} for testing purposes.
 * <p>
 * For example, printing stack traces on the server side is suppressed.
 *
 * @author Christian Kohlschütter
 */
public class TestingDumboServerService extends DumboServerServiceImpl {
  private final String randomKey = "testing-" + UUID.randomUUID().toString();

  @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
  @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
  public TestingDumboServerService(Class<? extends DumboApplication> applicationClass)
      throws IOException, InterruptedException {
    super(applicationClass);

    DumboServerImpl server = getDumboServerImpl();
    server.setJsonRpcTestSecretConsumer(randomKey, (c) -> {
      c.setErrorStackTraces(false);
    });

    server.awaitIdle();
  }

  @Override
  protected <T> T decorateRpcClientProxy(T proxy) {
    if (proxy instanceof SessionAccess) {
      Session session = ((SessionAccess) proxy).getDumborbClientSession();
      if (session instanceof CustomHeaderURLConnectionSession) {
        ((CustomHeaderURLConnectionSession) session).setHeaderValue(randomKey);
      }
    }

    return super.decorateRpcClientProxy(proxy);
  }

  @Override
  protected void configureNewServerImpl(DumboServerImplBuilder builder) {
    builder.initFromEnvironmentVariables();
    builder.enablePrewarm();
  }
}
