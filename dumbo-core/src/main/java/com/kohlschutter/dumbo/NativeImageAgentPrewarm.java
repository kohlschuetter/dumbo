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

import java.nio.file.Path;

import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboContentBuilder;

/**
 * "Prewarms" GraalVM native-image-agent, so it learns about reflective access, etc., necessary for
 * some Dumbo application.
 *
 * @author Christian Kohlschütter
 */
public class NativeImageAgentPrewarm {
  private static String getRequiredProperty(String prop) {
    String v = System.getProperty(prop);
    if (v == null || v.isEmpty()) {
      throw new IllegalArgumentException("Missing property: " + prop);
    }
    return v;
  }

  private static Path toPath(String prop) {
    String v = System.getProperty(prop);
    if (v == null || v.isEmpty()) {
      return null;
    }
    return Path.of(v);
  }

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    String application = getRequiredProperty("dumbo.prewarm.app");
    Class<?> appClass = Class.forName(application);

    Path jspClassesOutPath = toPath("dumbo.prewarm.jsp-classes-out");
    Path jspSourcesOutPath = toPath("dumbo.prewarm.jsp-sources-out");

    if (jspClassesOutPath != null) {
      System.out.println("Writing JSP classes to: " + jspClassesOutPath);
    }
    if (jspSourcesOutPath != null) {
      System.out.println("Writing JSP sources to: " + jspSourcesOutPath);
    }

    DumboContentBuilder.begin() //
        .withApplication((Class<? extends DumboApplication>) appClass) //
        .withJspClassOutputPath(jspClassesOutPath)//
        .withJspSourceOutputPath(jspSourcesOutPath)//
        .withVisitRelativeURL(args) //
        .build();
  }
}
