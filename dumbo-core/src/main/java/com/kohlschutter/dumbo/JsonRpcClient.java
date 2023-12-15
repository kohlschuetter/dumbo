package com.kohlschutter.dumbo;

import java.net.URL;

import com.kohlschutter.dumborb.client.Client;
import com.kohlschutter.dumborb.client.CustomHeaderURLConnectionSession;
import com.kohlschutter.dumborb.security.ClassResolver;

public final class JsonRpcClient extends Client {

  public JsonRpcClient(URL url, ClassResolver resolver) {
    super(new DumboURLConnectionSession(url), resolver);
  }

  public void setTestSecret(String secret) {
    ((CustomHeaderURLConnectionSession) getDumborbClientSession()).setHeaderValue(secret);
  }
}
