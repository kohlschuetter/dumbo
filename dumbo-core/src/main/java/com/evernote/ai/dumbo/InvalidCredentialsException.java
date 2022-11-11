package com.evernote.ai.dumbo;

public class InvalidCredentialsException extends IllegalStateException {
  private static final long serialVersionUID = 1L;

  public InvalidCredentialsException() {
    super();
  }

  public InvalidCredentialsException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidCredentialsException(String s) {
    super(s);
  }

  public InvalidCredentialsException(Throwable cause) {
    super(cause);
  }
}
