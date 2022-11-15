package com.kohlschutter.dumbo.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;

public abstract class MultiplexingConsole implements Console {
  public MultiplexingConsole() {

  }

  @Override
  public void add(Object o) {
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
  public void shutdown(ShutdownNotice notice) {
    for (Console c : getConsoles()) {
      c.shutdown(notice);
    }
  }
}
