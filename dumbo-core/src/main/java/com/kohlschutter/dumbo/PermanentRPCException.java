package com.kohlschutter.dumbo;

public class PermanentRPCException extends IllegalStateException {
  private static final long serialVersionUID = 1L;

  public PermanentRPCException() {
    super();
  }

  public PermanentRPCException(String message, Throwable cause) {
    super(message, cause);
  }

  public PermanentRPCException(String message) {
    super(message);
  }

  public PermanentRPCException(Throwable cause) {
    super(cause);
  }

}
