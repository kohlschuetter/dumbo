/*
 * Copyright 2022,2023 Christian Kohlschütter
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
package com.kohlschutter.dumbo;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.kohlschutter.dumbo.api.Console;
import com.kohlschutter.dumbo.api.DumboSession;
import com.kohlschutter.dumbo.console.ClearConsole;
import com.kohlschutter.dumbo.console.ConsoleService;

/**
 * An object-oriented console ("System.out"), which can be controlled via RPC.
 *
 * Content can be sent directly as a series objects (which must be marshallable via RPC), or through
 * a {@link PrintWriter} -- in the latter case output will be sent as chunks of strings.
 */
final class ConsoleImpl implements Console {
  private static final int MAX_CHUNKS_AT_ONCE = 20;
  private static final long MAX_WAIT_NEXT_CHUNK_MILLIS = 20 * 1000;

  private final StringWriter sw = new StringWriter() {
    @Override
    public void flush() {
      super.flush();
      synchronized (consoleService) {
        if (markedDontFlush) {
          return;
        }
        addChunkFromBufferToCache();
        consoleService.notifyAll();
      }
    }
  };
  private final PrintWriter consoleOut = new PrintWriter(sw, true) {
    @Override
    public void close() {
    }
  };

  private final AtomicBoolean closed = new AtomicBoolean();
  private final AtomicBoolean shutdownNoticeSent = new AtomicBoolean();
  private ShutdownNotice shutdownRequested = null;

  // FIXME this should be a circular buffer of some large maximum size to prevent OOMEs
  private final List<Object> cachedChunks = Collections.synchronizedList(new LinkedList<>());
  private final DumboSession session;

  // private final Thread CHECK_UNCLEAN_SHUTDOWN = new Thread() {
  // @Override
  // public void run() {
  // ConsoleImpl.this.shutdown(ShutdownNotice.NOT_CLEAN);
  // synchronized (consoleService) {
  // try {
  // consoleService.wait(500);
  // Thread.sleep(500);
  // } catch (InterruptedException e) {
  // // ignore
  // }
  // }
  // }
  // };

  private final ConsoleService consoleService = new ConsoleService() {
    @Override
    public Object requestNextChunk() {
      return requestNextChunk(MAX_WAIT_NEXT_CHUNK_MILLIS);
    }

    private Object requestNextChunk(final long maxWait) {
      if (closed.get()) {
        return null;
      } else if (shutdownNoticeSent.get()) {
        close();
        return null;
      }

      synchronized (consoleService) {
        try {
          final int numChunks = cachedChunks.size();
          switch (numChunks) {
            case 0:
              Object chunk = ConsoleImpl.this.getChunkFromBuffer();
              if ("".equals(chunk) && maxWait > 0) {
                try {
                  consoleService.wait(maxWait);
                  chunk = requestNextChunk(0);
                } catch (InterruptedException ignore) {
                  // ignore
                }
              }
              return chunk;
            case 1:
              return cachedChunks.remove(0);
            default:
              List<Object> head = cachedChunks.subList(0, Math.min(MAX_CHUNKS_AT_ONCE, cachedChunks
                  .size()));
              Object[] chunks = head.toArray();
              head.clear();
              return new MultipleChunks(chunks);
          }
        } finally {
          consoleService.notifyAll();
        }
      }
    }
  };

  private boolean markedDontFlush = false;

  /**
   * Creates a new {@link ConsoleImpl}.
   */
  ConsoleImpl(DumboSession session) {
    this.session = session;
    // app.registerCloseable(this);

    // Runtime.getRuntime().addShutdownHook(CHECK_UNCLEAN_SHUTDOWN);
  }

  ConsoleService getConsoleService() {
    return consoleService;
  }

  private Object getChunkFromBuffer() {
    synchronized (consoleService) {
      if (markedDontFlush) {
        if (shutdownRequested != null) {
          shutdownNoticeSent.set(true);
          session.invalidate();
          return shutdownRequested;
        }
        return "";
      }

      StringBuffer buffer = sw.getBuffer();
      if (buffer.length() == 0) {
        if (shutdownRequested != null) {
          shutdownNoticeSent.set(true);
          session.invalidate();
          return shutdownRequested;
        }
        return "";
      }
      String chunk = buffer.toString();
      buffer.setLength(0);
      return chunk;
    }
  }

  private void addChunkFromBufferToCache() {
    Object obj = getChunkFromBuffer();
    if (obj != null && !"".equals(obj)) {
      cachedChunks.add(obj);
    }
  }

  /**
   * Suspends flushing any chunks to the client, until {@link #resumeFlushing()} is called again.
   *
   * This can be used to logically group content before sending it to the client.
   *
   * @throws IOException on error.
   * @see #resumeFlushing()
   */
  public void suspendFlushing() throws IOException {
    synchronized (consoleService) {
      checkClosed();
      if (markedDontFlush) {
        return;
      }
      addChunkFromBufferToCache();
      markedDontFlush = true;
      consoleService.notifyAll();
    }
  }

  /**
   * Resumes flushing chunks to the client again, after it was suspended by calling
   * {@link #suspendFlushing()}
   *
   * This can be used to logically group content before sending it to the client.
   *
   * @throws IOException on error.
   * @see #suspendFlushing()
   */
  public void resumeFlushing() throws IOException {
    synchronized (consoleService) {
      checkClosed();
      if (!markedDontFlush) {
        return;
      }
      markedDontFlush = false;
      addChunkFromBufferToCache();
      consoleService.notifyAll();
    }
  }

  /**
   * Adds some data to the output.
   *
   * @param o The object to be added to the output.
   */
  @Override
  public void add(Object o) {
    synchronized (consoleService) {
      if (closed.get() || o == null || (shutdownRequested != null)) {
        return;
      }
      addChunkFromBufferToCache();
      cachedChunks.add(o);
      consoleService.notifyAll();
    }
  }

  /**
   * Convenience method for {@code getPrintWriter().println(s);}.
   *
   * @param s The string to print.
   */
  @Override
  public void println(String s) {
    consoleOut.println(s);
  }

  /**
   * Convenience method for {@code getPrintWriter().println(o);}.
   *
   * @param o The object to print.
   */
  @Override
  public void println(Object o) {
    consoleOut.println(o);
  }

  /**
   * Convenience method for {@code getPrintWriter().println();}.
   */
  @Override
  public void println() {
    consoleOut.println();
  }

  /**
   * Returns a {@link PrintWriter} that allows the textual data to be sent as String objects.
   *
   * @return This console's {@link PrintWriter}.
   */
  @Override
  public PrintWriter getPrintWriter() {
    return consoleOut;
  }

  /**
   * Returns {@code true} if this {@link ConsoleImpl} has been closed.
   *
   * @return {@code true} if closed.
   */
  public boolean isClosed() {
    return closed.get();
  }

  /**
   * Checks whether this {@link ConsoleImpl} has been closed. An {@link IOException} is thrown in
   * this case.
   *
   * @throws IOException if closed.
   */
  public void checkClosed() throws IOException {
    if (isClosed()) {
      throw new IOException("Console is closed");
    }
  }

  @Override
  public void close() {
    synchronized (consoleService) {
      if (!closed.compareAndSet(false, true)) {
        return;
      }
    }
    consoleOut.close();
    sw.getBuffer().setLength(0);
  }

  /**
   * Requests the application to gracefully shutdown.
   */
  @Override
  public void shutdown() {
    shutdown(ShutdownNotice.CLEAN);
  }

  /**
   * Requests the application to gracefully shutdown.
   */
  @Override
  public void shutdown(boolean clean) {
    shutdown(clean ? ShutdownNotice.CLEAN : ShutdownNotice.NOT_CLEAN);
  }

  public void shutdown(ShutdownNotice notice) {
    if (shutdownNoticeSent.get()) {
      return;
    }
    synchronized (consoleService) {
      if (isClosed()) {
        return;
      }
      if (shutdownRequested == null) {
        shutdownRequested = notice;
      }
      consoleService.notifyAll();
    }
  }

  /**
   * Tells the app to clear the console.
   */
  @Override
  public void clear() {
    add(new ClearConsole());
  }
}
