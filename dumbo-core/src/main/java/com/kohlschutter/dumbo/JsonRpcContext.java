package com.kohlschutter.dumbo;

public final class JsonRpcContext {
  private boolean errorStackTraces = true;
  private final String method;

  JsonRpcContext(String method) {
    this.method = method;
  }

  public String getMethod() {
    return method;
  }

  public boolean isErrorStackTraces() {
    return errorStackTraces;
  }

  public void setErrorStackTraces(boolean errorStackTraces) {
    this.errorStackTraces = errorStackTraces;
  }
}
