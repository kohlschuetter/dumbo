/*
 * Copyright 2022-2025 Christian Kohlschütter
 * Copyright 2014,2015 Evernote Corporation.
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
package com.kohlschutter.dumbo.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;

import com.kohlschutter.dumbo.api.Console;

public abstract class MultiplexingConsole implements Console {
  public MultiplexingConsole() {

  }

  @Override
  public void add(Object... o) {
    for (Console c : getConsoles()) {
      c.add(o);
    }
  }

  @Override
  public void println(String s) {
    for (Console c : getConsoles()) {
      c.println(s);
    }
  }

  @Override
  public void println(Object o) {
    for (Console c : getConsoles()) {
      c.println(o);
    }
  }

  @Override
  public void println() {
    for (Console c : getConsoles()) {
      c.println();
    }
  }

  @Override
  public void clear() {
    for (Console c : getConsoles()) {
      c.clear();
    }
  }

  protected abstract Collection<Console> getConsoles();

  @Override
  public PrintWriter getPrintWriter() {
    return new PrintWriter(new Writer() {

      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        for (Console c : getConsoles()) {
          c.getPrintWriter().write(cbuf, off, len);
        }
      }

      @Override
      public void flush() throws IOException {
        for (Console c : getConsoles()) {
          c.getPrintWriter().flush();
        }
      }

      @Override
      public void close() throws IOException {
        flush();
      }
    }) {
      @Override
      public boolean checkError() {
        if (super.checkError()) {
          return true;
        }
        for (Console c : getConsoles()) {
          if (c.getPrintWriter().checkError()) {
            return true;
          }
        }
        return false;
      }
    };
  }

  @Override
  public void shutdown() {
    for (Console c : getConsoles()) {
      c.shutdown();
    }
  }

  @Override
  public void shutdown(boolean clean) {
    for (Console c : getConsoles()) {
      c.shutdown(clean);
    }
  }
}
