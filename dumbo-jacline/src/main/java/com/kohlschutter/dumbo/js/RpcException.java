package com.kohlschutter.dumbo.js;

public class RpcException extends Exception {
  private static final long serialVersionUID = 1L;
  private final int code;
  private final String data;

  public RpcException(String message, int code, String data) {
    super(message);
    this.code = code;
    this.data = data;
  }

  public int getCode() {
    return code;
  }

  public String getData() {
    return data;
  }
}
