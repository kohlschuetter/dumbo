/*
 * dumbo-markdown
 *
 * Copyright 2022,2023 Christian Kohlschütter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kohlschutter.dumbo.markdown.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.kohlschutter.annotations.compiletime.ExcludeFromCodeCoverageGeneratedReport;
import com.kohlschutter.stringhold.HasLength;
import com.kohlschutter.stringhold.HasMinimumLength;
import com.kohlschutter.stringhold.IOSupplier;

/**
 * A collection of useful {@link Reader}-related operations for
 * {@link IOSupplier}&lt;{@link Reader}&gt;.
 *
 * @author Christian Kohlschütter
 */
final class IOReaderSupplier {
  @ExcludeFromCodeCoverageGeneratedReport(reason = "unreachable")
  private IOReaderSupplier() {
    throw new IllegalStateException("No instances");
  }

  /**
   * Returns an {@link IOSupplier} providing {@link Reader} instances with the given content.
   *
   * @param s The content to provide.
   * @return The supplier.
   */
  public static IOSupplier<Reader> withContentsOf(String s) {
    return () -> LengthAwareStringReader.withString(s);
  }

  /**
   * Returns an {@link IOSupplier} providing {@link Reader} instances with content from the given
   * file, using the given {@link Charset}.
   *
   * @param f The file to provide contents from.
   * @param cs The {@link Charset} to use.
   * @return The supplier.
   */
  static IOSupplier<Reader> withContentsOf(File f, Charset cs) {
    return new IOSupplier<Reader>() {
      @Override
      public Reader get() throws IOException {
        long lenLong = f.length();
        int len = lenLong >= Integer.MAX_VALUE ? -1 : (int) lenLong;
        // NOTE this assumes that the length doesn't change between these two calls (hard to
        // enforce)
        FileInputStream in = new FileInputStream(f);
        if (len < 0) {
          return new InputStreamReader(in, cs);
        } else if (len == 0) {
          in.close();
          return LengthAwareStringReader.withString("");
        } else {
          // FIXME Byte-Order-Mark (BOM) detection etc.

          if (StandardCharsets.ISO_8859_1.equals(cs) || StandardCharsets.US_ASCII.equals(cs)) {
            return new LengthAwareInputStreamReader(in, cs, len);
          } else {
            return new MinimumLengthAwareInputStreamReader(in, cs, len);
          }
        }
      }

      @Override
      public String toString() {
        return super.toString() + "[file=" + f + ";charset=" + cs + "]";
      }
    };
  }

  /**
   * Returns an {@link IOSupplier} providing {@link Reader} instances with content from file at the
   * given path, using the given {@link Charset}.
   *
   * @param p The path to the file to provides contents from.
   * @param cs The {@link Charset} to use.
   * @return The supplier.
   */
  static IOSupplier<Reader> withContentsOf(Path f, Charset cs) {
    return new IOSupplier<Reader>() {
      @Override
      public Reader get() throws IOException {
        long lenLong = Files.size(f);
        int len = lenLong >= Integer.MAX_VALUE ? -1 : (int) lenLong;
        // NOTE this assumes that the length doesn't change between these two calls (hard to
        // enforce)

        InputStream in = Files.newInputStream(f);
        if (len < 0) {
          return new InputStreamReader(in, cs);
        } else if (len == 0) {
          in.close();
          return LengthAwareStringReader.withString("");
        } else {
          // FIXME Byte-Order-Mark (BOM) detection etc.

          if (StandardCharsets.ISO_8859_1.equals(cs) || StandardCharsets.US_ASCII.equals(cs)) {
            return new LengthAwareInputStreamReader(in, cs, len);
          } else {
            return new MinimumLengthAwareInputStreamReader(in, cs, len);
          }
        }
      }

      @Override
      public String toString() {
        return super.toString() + "[file=" + f + ";charset=" + cs + "]";
      }
    };
  }

  /**
   * Returns an {@link IOSupplier} providing {@link Reader} instances with content from the given
   * URL, using the given {@link Charset}.
   *
   * @param url The URL to provide contents from.
   * @param cs The {@link Charset} to use.
   * @return The supplier.
   */
  static IOSupplier<Reader> withContentsOf(URL url, Charset cs) {
    return new IOSupplier<Reader>() {
      @Override
      public Reader get() throws IOException {
        URLConnection conn = url.openConnection();
        InputStream in = url.openStream();
        int len = conn.getContentLength(); // in bytes
        if (len < 0) {
          return new InputStreamReader(in, cs);
        } else if (len == 0) {
          in.close();
          return LengthAwareStringReader.withString("");
        } else {
          // FIXME Byte-Order-Mark (BOM) detection etc.

          if (StandardCharsets.ISO_8859_1.equals(cs) || StandardCharsets.US_ASCII.equals(cs)) {
            return new LengthAwareInputStreamReader(in, cs, len);
          } else {
            return new MinimumLengthAwareInputStreamReader(in, cs, len);
          }
        }
      }

      @Override
      public String toString() {
        return super.toString() + "[url=" + url + ";charset=" + cs + "]";
      }

    };
  }

  /**
   * An {@link InputStreamReader} that has a known minimum string length associated.
   *
   * @author Christian Kohlschütter
   */
  static class MinimumLengthAwareInputStreamReader extends InputStreamReader implements
      HasMinimumLength {
    private final int minLen;

    /**
     * Constructs a new {@link MinimumLengthAwareInputStreamReader}.
     *
     * @param in The wrapped {@link InputStream}.
     * @param cs The {@link Charset} to use for conversion.
     * @param len The expected minimum length (in characters).
     * @throws IllegalArgumentException on negative length.
     */
    MinimumLengthAwareInputStreamReader(InputStream in, Charset cs, int len) {
      super(in, cs);
      if (len < 0) {
        throw new IllegalArgumentException("len");
      }
      this.minLen = len;
    }

    @Override
    public int getMinimumLength() {
      return minLen;
    }
  }

  /**
   * An {@link InputStreamReader} that has a known string length associated.
   *
   * @author Christian Kohlschütter
   */
  static final class LengthAwareInputStreamReader extends MinimumLengthAwareInputStreamReader
      implements HasLength {
    private final int len;

    /**
     * Constructs a new {@link LengthAwareInputStreamReader}.
     *
     * @param in The wrapped {@link InputStream}.
     * @param cs The {@link Charset} to use for conversion.
     * @param len The expected length (in characters).
     * @throws IllegalArgumentException on negative length.
     */
    LengthAwareInputStreamReader(InputStream in, Charset cs, int len) {
      super(in, cs, len);
      if (len < 0) {
        throw new IllegalArgumentException("len");
      }
      this.len = len;
    }

    @Override
    public int length() {
      return len;
    }
  }

  /**
   * A {@link StringReader} that provides the string's length.
   *
   * @author Christian Kohlschütter
   */
  static final class LengthAwareStringReader extends StringReader implements HasLength {
    private final String string;

    private LengthAwareStringReader(String s) {
      super(s);
      this.string = s;
    }

    /**
     * Returns the a length-aware StringReader for the given string.
     *
     * @param s The string to wrap.
     * @return The length-aware {@link StringReader}.
     */
    static StringReader withString(String s) {
      return new LengthAwareStringReader(s);
    }

    @Override
    public int length() {
      return string.length();
    }
  }
}
