package com.kohlschutter.dumbo.console;

import java.io.Closeable;
import java.io.PrintWriter;

public interface Console extends Closeable {
  /**
   * The "shutdown notice" that is sent to the client.
   */
  public static final class ShutdownNotice {
    public static final ShutdownNotice CLEAN = new ShutdownNotice(true);
    public static final ShutdownNotice NOT_CLEAN = new ShutdownNotice(false);

    private boolean clean;

    private ShutdownNotice(boolean clean) {
      this.clean = clean;
    }

    /**
     * If {@code true}, consider this shutdown "clean". If {@code false}, assume there was an error.
     *
     * @return The "clean" state.
     */
    public boolean isClean() {
      return clean;
    }
  }

  /**
   * Adds some data to the output.
   *
   * @param o The object to be added to the output.
   */
  void add(Object o);

  /**
   * Convenience method for {@code getPrintWriter().println(s);}
   *
   * @param s The string to print.
   */
  void println(String s);

  /**
   * Convenience method for {@code getPrintWriter().println(o);}
   *
   * @param o The object to print.
   */
  void println(Object o);

  /**
   * Convenience method for {@code getPrintWriter().println();}
   */
  void println();

  /**
   * Tells the app to clear the console.
   */
  void clear();

  /**
   * Returns a {@link PrintWriter} that allows the textual data to be sent as String objects.
   *
   * @return This console's {@link PrintWriter}.
   */
  PrintWriter getPrintWriter();

  /**
   * Requests the application to gracefully shutdown.
   */
  void shutdown();

  /**
   * Requests the application to gracefully shutdown.
   */
  void shutdown(ShutdownNotice notice);
}
