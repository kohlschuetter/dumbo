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
package com.kohlschutter.dumbo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class ThreadLocalRequestAccess {
  private static final ThreadLocal<HttpServletRequest> TL_REQUEST = new ThreadLocal<>();

  private ThreadLocalRequestAccess() {
  }

  static HttpServletRequest getHttpServletRequest() {
    return TL_REQUEST.get();
  }

  static void setHttpServletRequest(HttpServletRequest req) {
    TL_REQUEST.set(req);
  }

  public static HttpSession getHttpSession(boolean create) {
    HttpServletRequest req = getHttpServletRequest();
    if (req == null) {
      if (create) {
        throw new IllegalStateException("Not in a request");
      }
      return null;
    } else {
      return req.getSession(create);
    }
  }
}
