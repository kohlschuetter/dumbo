package com.kohlschutter.dumbo.jacline.helloworld;

import java.nio.file.Files;
import java.nio.file.Path;

final class Locations {
  private static final boolean AVAILABLE;
  private static final Path BASE_DIR = Path.of("/Volumes/RAM-Disk/dumbo.tmp");
  private static final Path STATIC_OUT = BASE_DIR.resolve("static");
  private static final Path DYNAMIC_OUT = BASE_DIR.resolve("dynamic");

  static {
    Path parentPath = BASE_DIR.getParent();
    AVAILABLE = parentPath == null || Files.isDirectory(parentPath);
  }

  public static boolean isAvailable() {
    return AVAILABLE;
  }

  public static Path getStaticOut() {
    return STATIC_OUT;
  }

  public static Path getDynamicOut() {
    return DYNAMIC_OUT;
  }
}
