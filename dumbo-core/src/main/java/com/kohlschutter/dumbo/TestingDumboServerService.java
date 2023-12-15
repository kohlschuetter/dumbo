package com.kohlschutter.dumbo;

import java.io.IOException;
import java.util.UUID;

import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumborb.client.CustomHeaderURLConnectionSession;
import com.kohlschutter.dumborb.client.Session;
import com.kohlschutter.dumborb.client.SessionAccess;

public class TestingDumboServerService extends DumboServerService {
  private final String randomKey = "testing-" + UUID.randomUUID().toString();

  public TestingDumboServerService(Class<? extends DumboApplication> applicationClass)
      throws IOException, InterruptedException {
    super(applicationClass);

    getServer().setJsonRpcTestSecretConsumer(randomKey, (c) -> {
      c.setErrorStackTraces(false);
    });
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
}
