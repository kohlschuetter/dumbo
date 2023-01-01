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
package com.kohlschutter.dumbo.markdown.site;

import java.util.Map;

public final class CustomSiteVariables {
  public static final String DUMBO_RELATIVE_PATH = " __dumbo_path";
  public static final String DUMBO_FILENAME = " __dumbo_filename";

  private CustomSiteVariables() {
  }

  public static void storePathAndFilename(String relativePath, Map<String, Object> variablesOut) {
    variablesOut.put(CustomSiteVariables.DUMBO_RELATIVE_PATH, relativePath);

    int lastSlash = relativePath.lastIndexOf('/');
    String filename;
    if (lastSlash >= 0) {
      filename = relativePath.substring(lastSlash + 1);
    } else {
      filename = relativePath;
    }
    variablesOut.put(CustomSiteVariables.DUMBO_FILENAME, filename);
  }

  public static void copyPathAndFileName(Map<String, Object> from, Map<String, Object> to) {
    to.put(DUMBO_FILENAME, from.get(CustomSiteVariables.DUMBO_FILENAME));
    to.put(DUMBO_RELATIVE_PATH, from.get(CustomSiteVariables.DUMBO_RELATIVE_PATH));
  }
}
