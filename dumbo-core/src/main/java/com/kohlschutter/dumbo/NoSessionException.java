package com.kohlschutter.dumbo;

public class NoSessionException extends PermanentRPCException {
  private static final long serialVersionUID = 1L;

  public NoSessionException() {
    super();
  }

  public NoSessionException(String message, Throwable cause) {
    super(message, cause);
  }

  public NoSessionException(String message) {
    super(message);
  }

  public NoSessionException(Throwable cause) {
    super(cause);
  }
}
