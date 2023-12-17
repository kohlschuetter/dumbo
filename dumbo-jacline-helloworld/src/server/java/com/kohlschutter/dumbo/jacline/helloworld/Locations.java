package com.kohlschutter.dumbo.jacline.helloworld;

import java.nio.file.Files;
import java.nio.file.Path;

final class Locations {
  private static final boolean AVAILABLE;
  private static final Path BASE_DIR = Path.of("dumbo-out");

  static {
    Path parentPath = BASE_DIR.getParent();
    AVAILABLE = parentPath == null || Files.isDirectory(parentPath);
  }

  public static boolean isAvailable() {
    return AVAILABLE;
  }

  public static Path getOutputPath() {
    return BASE_DIR;
  }
}
