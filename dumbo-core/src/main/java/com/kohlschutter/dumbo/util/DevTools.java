/*
 * Copyright 2022 Christian Kohlschütter
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
package com.kohlschutter.dumbo.util;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.kohlschutter.dumbo.AppHTTPServer;

/**
 * Helper methods for developing. Currently works on OS X only.
 */
public final class DevTools {
  private static final Logger LOG = Logger.getLogger(DevTools.class);
  private static boolean staticMode;

  private DevTools() {
    throw new IllegalStateException("No instances");
  }

  private static final String PATH_TO_MODIFIERKEYS;
  static {
    String path = null;
    for (String p : new String[] {"/usr/local/bin/modifierkeys"}) {
      if (new File(p).canExecute()) {
        path = p;
        break;
      }
    }

    PATH_TO_MODIFIERKEYS = path;
  }

  public static boolean isShiftPressed() {
    if (PATH_TO_MODIFIERKEYS == null) {
      // Cannot check
      return false;
    }

    try {
      Process proc = Runtime.getRuntime().exec(PATH_TO_MODIFIERKEYS);
      int rc = proc.waitFor();
      return (rc & 2) != 0;
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
    }

    return false;
  }

  /**
   * Opens some URL, using some system API.
   *
   * FIXME: This is currently implemented for macOS only.
   *
   * @param url The URL to open.
   * @throws IOException on error.
   */
  public static void openURL(String url) throws IOException {
    Runtime.getRuntime().exec(new String[] {"/usr/bin/open", url});
  }

  public static void openURL(AppHTTPServer server, String page) {
    try {
      String url = server.getURI().toString().replaceFirst("/$", "") + "/" + page.replaceFirst("^/",
          "");

      if (staticMode) {
        url += "?static";
      }

      LOG.info("Opening page in browser: " + url);
      DevTools.openURL(url);
    } catch (Exception e) {
      LOG.error(e);
    }
  }

  public static void init() {
    staticMode = DevTools.isShiftPressed();
    if (staticMode) {
      LOG.info("Shift press detected -- enabling static design mode.");
    } else {
      LOG.info("Running in live mode. Start server with \"shift\" key pressed to "
          + "enable static design mode.");
    }
  }
}