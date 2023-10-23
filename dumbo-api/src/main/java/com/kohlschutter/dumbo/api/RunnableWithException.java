package com.kohlschutter.dumbo.api;

/**
 * A {@link Runnable} that can throw an {@link Exception}.
 * 
 * @author Christian Kohlschütter
 */
@FunctionalInterface
public interface RunnableWithException {
  void run() throws Exception;
}
