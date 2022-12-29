package com.kohlschutter.dumbo.markdown.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.kohlschutter.stringhold.IOSupplier;

public class PathReaderSupplier implements IOSupplier<Reader> {
  private final IOSupplier<Reader> supplier;
  private final String relativePath;
  private final String type;

  private PathReaderSupplier(IOSupplier<Reader> supplier, String relativePath, String type) {
    this.supplier = supplier;
    this.relativePath = relativePath;
    this.type = type;
  }

  public static PathReaderSupplier withContentsOf(String relativePath, File f, Charset cs) {
    return withContentsOf(null, relativePath, f, cs);
  }

  public static PathReaderSupplier withContentsOf(String type, String relativePath, File f,
      Charset cs) {
    return new PathReaderSupplier(IOReaderSupplier.withContentsOf(f, StandardCharsets.UTF_8),
        relativePath, type);
  }

  public static PathReaderSupplier withContentsOf(String type, String relativePath, URL u,
      Charset cs) {
    return new PathReaderSupplier(IOReaderSupplier.withContentsOf(u, StandardCharsets.UTF_8),
        relativePath, type);
  }

  @Override
  public Reader get() throws IOException {
    return supplier.get();
  }

  public String getRelativePath() {
    return relativePath;
  }

  public String getType() {
    return type;
  }
}
