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
package com.kohlschutter.dumbo.markdown;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds files in the resource classpath.
 *
 * @author Christian Kohlschütter
 */
public class ResourcePathTraverser {
  private static final Pattern PAT_META_INF_VERSIONS_PREFIX = Pattern.compile(
      "META-INF/versions/[0-9]+/");

  /**
   * Finds resources located along the given resource (identified by class and base reference path),
   * returning them as a Map of path (relative to the reference point) to URL.
   *
   * @param clazz The class reference.
   * @param baseRefPath The base reference path.
   * @param includeDirectories Whether directories should be included in the output (names ending
   *          with {@code /}).
   * @param filter The filter that decides which results should be returned.
   * @return The map.
   * @throws IOException on error.
   */
  public static final Map<String, URL> findURLs(Class<?> clazz, String baseRefPath,
      boolean includeDirectories, Predicate<String> filter) throws IOException {
    URL url = clazz.getResource(baseRefPath);
    if (url == null) {
      return Collections.emptyMap();
    }
    URL baseUrl = clazz.getResource("");
    String baseUrlStr = baseUrl.toString();
    String urlStr = url.toString();

    if (!urlStr.startsWith(baseUrlStr)) {
      throw new IOException("Unexpected class base URL");
    }

    String protocol = url.getProtocol();
    if (baseRefPath.startsWith("/") || baseRefPath.contains("..")) {
      throw new IOException("Illegal baseRef");
    }
    List<Entry<String, URL>> list;
    if ("jar".equals(protocol)) {
      list = findURLsFromJar(baseUrlStr, url, baseRefPath, includeDirectories, filter);
    } else if ("file".equals(protocol)) {
      list = findURLsFromFileURL(baseUrlStr, url, clazz, baseRefPath, includeDirectories, filter);
    } else {
      throw new IOException("Unsupported protocol: " + protocol);
    }

    if (list.isEmpty()) {
      return Collections.emptyMap();
    }

    LinkedHashMap<String, URL> map = new LinkedHashMap<>(list.size());
    for (Map.Entry<String, URL> en : list) {
      map.put(en.getKey(), en.getValue());
    }
    return map;
  }

  private static File fromFileURL(URL u) throws IOException {
    if (!"file".equals(u.getProtocol())) {
      throw new IOException();
    }
    try {
      return new File(u.toURI());
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  private static String toPrefixString(File f) {
    String str = f.toString();
    if (f.isDirectory() && !str.endsWith(File.separator)) {
      str += File.separator;
    }
    return str;
  }

  private static List<Map.Entry<String, URL>> findURLsFromFileURL(String baseUrlStr, URL url,
      Class<?> clazz, String baseRef, boolean includeDirectories, Predicate<String> filter)
      throws IOException {
    File f = fromFileURL(url);
    if (!f.exists()) {
      return Collections.emptyList();
    } else if (f.isFile()) {
      // always start with a directory
      f = f.getParentFile();
    }

    String basePrefix = toPrefixString(f);

    List<Map.Entry<String, URL>> list = new ArrayList<>();

    List<File> filesToVisit = new ArrayList<>();
    for (File subdirFile : f.listFiles()) {
      filesToVisit.add(subdirFile);
    }

    int numFilesToProcess;
    while ((numFilesToProcess = filesToVisit.size()) > 0) {
      for (int i = 0; i < numFilesToProcess; i++) {
        File v = filesToVisit.get(i);
        if (!v.exists()) {
          continue;
        }

        String vPath = toPrefixString(v);
        if (!vPath.startsWith(basePrefix)) {
          throw new IOException("Unexpected path mismatch");
        }

        String filterName = vPath.substring(basePrefix.length());

        boolean isDir = v.isDirectory();
        if (!isDir && !filter.test(filterName)) {
          continue;
        }

        if (!isDir || includeDirectories) {
          try {
            list.add(new AbstractMap.SimpleEntry<>(filterName, v.toURI().toURL()));
          } catch (MalformedURLException e) {
            throw new IOException(e);
          }
        }

        if (v.isDirectory()) {
          for (File subdirFile : v.listFiles()) {
            filesToVisit.add(subdirFile);
          }
        }
      }
      filesToVisit.subList(0, numFilesToProcess).clear();
    }

    list.sort((a, b) -> a.getKey().compareTo(b.getKey()));

    return list;
  }

  private static final List<Map.Entry<String, URL>> findURLsFromJar(String baseUrlStr, URL url,
      String baseRef, boolean includeDirectories, Predicate<String> filter) throws IOException {
    String path = url.getPath();
    if (!path.startsWith("file:")) {
      throw new IOException("Not a jar:file: URL");
    }

    int separatorStart = path.indexOf('!');
    if (separatorStart == -1 || path.length() < separatorStart + 2) {
      // bail
    }
    String jarUrl = path.substring(0, separatorStart);

    String prefix;
    int separatorEnd;
    if (path.charAt(separatorStart + 1) == '/') {
      separatorEnd = separatorStart + 2;
    } else {
      separatorEnd = separatorStart + 1;
    }
    prefix = path.substring(0, separatorEnd);
    path = path.substring(separatorEnd);
    if (!path.endsWith("/")) {
      int lastSlash = path.lastIndexOf('/');
      if (lastSlash == -1) {
        throw new IOException("Not slash in path: " + path);
      }
      path = path.substring(0, lastSlash + 1);
    }
    Matcher m = PAT_META_INF_VERSIONS_PREFIX.matcher(path);
    if (m.find() && m.start() == 0) {
      path = m.replaceFirst("");
      m = PAT_META_INF_VERSIONS_PREFIX.matcher(baseUrlStr);
      baseUrlStr = m.replaceFirst("");
    }

    URI uri;
    try {
      uri = new URI(jarUrl);
    } catch (URISyntaxException e) {
      throw new IOException("Invalid URI", e);
    }

    File f = new File(uri);
    if (!f.exists()) {
      return Collections.emptyList();
    }

    try (JarFile jf = new JarFile(f)) {
      List<Map.Entry<String, String>> list = new ArrayList<>();

      for (Enumeration<JarEntry> en = jf.entries(); en.hasMoreElements();) {
        JarEntry je = en.nextElement();
        String name = je.getRealName();
        if (name.length() > path.length() && name.startsWith(path) && !name.endsWith(".class")) {
          if (!includeDirectories && name.endsWith("/")) {
            continue;
          }

          name = "jar:" + prefix + name;
          if (!name.startsWith(baseUrlStr)) {
            throw new IOException("Unexpected jar URL mismatch");
          }

          // hello/world.txt
          String filterName = name.substring(baseUrlStr.length());

          if (filter.test(filterName)) {
            list.add(new AbstractMap.SimpleEntry<>(filterName, name));
          }
        }
      }
      list.sort((a, b) -> a.getKey().compareTo(b.getKey()));

      @SuppressWarnings("unchecked")
      List<Map.Entry<String, URL>> list2 = (List<Map.Entry<String, URL>>) (List<?>) list;
      for (int i = 0, n = list.size(); i < n; i++) {
        Map.Entry<String, String> s = list.get(i);

        @SuppressWarnings("unchecked")
        Map.Entry<String, URL> s2 = (Map.Entry<String, URL>) (Map.Entry<?, ?>) s;
        s2.setValue(new URL(s.getValue()));

        list2.set(i, s2);
      }
      return list2;
    }
  }
}
