package com.kohlschutter.dumbo;

/**
 * The "shutdown notice" that is sent to the client.
 * 
 * NOTE: Do not rename/move this class without changing app-console.js.
 */
public final class ShutdownNotice {
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
