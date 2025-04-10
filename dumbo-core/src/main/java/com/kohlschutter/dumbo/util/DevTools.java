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
package com.kohlschutter.dumbo.util;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;
import com.kohlschutter.dumbo.api.DumboServer;

/**
 * Helper methods for developing. Currently works on OS X only.
 */
public final class DevTools {
  private static final Logger LOG = LoggerFactory.getLogger(DevTools.class);

  private static final boolean SUPPORTED_OS = "Mac OS X".equals(System.getProperty("os.name"));

  private static boolean staticMode;
  private static final String PATH_TO_MODIFIERKEYS;

  static {
    String path = null;
    if (SUPPORTED_OS) {
      for (String p : new String[] {"/usr/local/bin/modifierkeys"}) {
        if (new File(p).canExecute()) {
          path = p;
          break;
        }
      }
    }
    PATH_TO_MODIFIERKEYS = path;
  }

  private DevTools() {
    throw new IllegalStateException("No instances");
  }

  public static boolean canDetectShiftPress() {
    return PATH_TO_MODIFIERKEYS != null;
  }

  public static boolean isShiftPressed() {
    if (PATH_TO_MODIFIERKEYS == null) {
      // Cannot check
      return false;
    }

    try {
      Process proc = Runtime.getRuntime().exec(new String[] {PATH_TO_MODIFIERKEYS});
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
  @SuppressFBWarnings("COMMAND_INJECTION")
  public static void openURL(String url) throws IOException {
    if (SUPPORTED_OS) {
      if (url.startsWith("http://") || url.startsWith("https://")) {
        LOG.info("Opening page in browser: {}", url);
        Runtime.getRuntime().exec(new String[] {"/usr/bin/open", url});
      } else {
        LOG.error("Cannot open URL in browser: {}", url);
      }
    } else {
      LOG.warn("Cannot page in browser (unsupported): {}", url);
    }
  }

  public static void openURL(DumboServer server) {
    openURL(server, "/");
  }

  public static void openURL(DumboServer server, String page) {
    try {
      String url = server.getNetworkURI().toString().replaceFirst("/$", "") + "/" + page
          .replaceFirst("^/", "");

      if (staticMode) {
        url += "?static";
      }

      DevTools.openURL(url);
    } catch (Exception e) {
      LOG.error("Can't open page in browser", e);
    }
  }

  public static void init() {
    if (!canDetectShiftPress()) {
      LOG.debug("DevTools unsupported in this environment");
      return;
    }
    staticMode = DevTools.isShiftPressed();
    if (staticMode) {
      LOG.warn("Shift press detected -- enabling static design mode.");
    } else {
      LOG.info("Running in live mode. Start server with \"shift\" key pressed to "
          + "enable static design mode.");
    }
  }
}
