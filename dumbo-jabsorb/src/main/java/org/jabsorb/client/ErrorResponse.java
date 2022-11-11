/*
 * jabsorb - a Java to JavaScript Advanced Object Request Broker
 * http://www.jabsorb.org
 *
 * Copyright 2007-2009 The jabsorb team
 *
 * based on original code from
 * JSON-RPC-Java - a JSON-RPC to Java Bridge with dynamic invocation
 *
 * Copyright Metaparadigm Pte. Ltd. 2004.
 * Michael Clark <michael@metaparadigm.com>
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
package org.jabsorb.client;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Exception created from the JSON-RPC error response
 */
public class ErrorResponse extends ClientError {
  /**
   * Generated id
   */
  private static final long serialVersionUID = 1L;
  private String message;
  private String trace;

  public ErrorResponse(Integer code, String message, String trace) {
    super(ErrorResponse.formatMessage(code));
    this.message = message;
    this.trace = trace;
  }

  private static String formatMessage(Integer code) {
    String result = code == null ? "JSONRPC error: " : "JSONRPC error code " + code.toString()
        + ": ";
    return result;
  }

  /** Borrowed from org.apache.commons.lang.exception.NestableDelegate */
  @Override
  public void printStackTrace(PrintStream out) {
    synchronized (out) {
      PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), false);
      printStackTrace(pw);
      // Flush the PrintWriter before it's GC'ed.
      pw.flush();
    }
  }

  @Override
  public void printStackTrace(PrintWriter s) {
    super.printStackTrace(s);
    if (message != null) {
      s.print("\nCaused by: ");
      s.println(message);
    }
    if (trace != null) {
      s.println();
      s.println(trace);
    }
  }
}
