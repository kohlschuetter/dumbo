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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kohlschutter.dumbo.util.SuppliedThreadLocal;

public final class PermalinkParser {
  private static final Pattern PAT_PERMA_VAR = Pattern.compile(":([a-z_]+)\\b");

  private static final Pattern PAT_DATE_FILENAME = Pattern.compile(
      "^([0-9]{4}-[0-9]{2}-[0-9]{2})-(.*?)$");
  private static final Pattern PAT_FILENAME_SLUG = Pattern.compile("^([^\\.]+)(\\..*?)?$");

  private static final Pattern PAT_ALPHANUM_LOWER = Pattern.compile("[^a-z0-9]+");

  private static final ThreadLocal<SimpleDateFormat> SDF_YMD = SuppliedThreadLocal.of(
      () -> new SimpleDateFormat("YYYY-MM-dd", Locale.ENGLISH));

  private static final Map<String, String> STYLES = new HashMap<>();

  static {
    STYLES.put("date", "/:categories/:year/:month/:day/:title:output_ext");
    STYLES.put("pretty", "/:categories/:year/:month/:day/:title/");
    STYLES.put("ordinal", "/:categories/:year/:y_day/:title:output_ext");
    STYLES.put("weekdate", "/:categories/:year/W:week/:short_day/:title:output_ext");
    STYLES.put("none", "/:categories/:title:output_ext");
  }

  private static final Map<String, SimpleDateFormat> DATE_KEYS = new HashMap<>();

  static {
    DATE_KEYS.put("year", new SimpleDateFormat("YYYY", Locale.ENGLISH));
    DATE_KEYS.put("short_year", new SimpleDateFormat("YY", Locale.ENGLISH));
    DATE_KEYS.put("month", new SimpleDateFormat("MM", Locale.ENGLISH));
    DATE_KEYS.put("i_month", new SimpleDateFormat("M", Locale.ENGLISH));
    DATE_KEYS.put("short_month", new SimpleDateFormat("MMM", Locale.ENGLISH));
    DATE_KEYS.put("long_month", new SimpleDateFormat("MMMM", Locale.ENGLISH));
    DATE_KEYS.put("day", new SimpleDateFormat("dd", Locale.ENGLISH));
    DATE_KEYS.put("i_day", new SimpleDateFormat("d", Locale.ENGLISH));
    DATE_KEYS.put("y_day", new SimpleDateFormat("D", Locale.ENGLISH));
    // FIXME add more here
    DATE_KEYS.put("hour", new SimpleDateFormat("HH", Locale.ENGLISH));
    DATE_KEYS.put("minute", new SimpleDateFormat("mm", Locale.ENGLISH));
    DATE_KEYS.put("second", new SimpleDateFormat("ss", Locale.ENGLISH));
  }

  private PermalinkParser() {
  }

  @SuppressWarnings({"PMD.NcssCount", "PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
  public static String parsePermalink(String permalink, Map<String, Object> pageVariables)
      throws ParseException {
    if (permalink == null) {
      return null;
    }
    if (!permalink.startsWith("/")) {
      String style = STYLES.get(permalink);
      if (style != null) {
        return parsePermalink(style, pageVariables);
      } else {
        throw new UnsupportedOperationException("Unsupported permalink style: " + permalink);
      }
    }

    if (!permalink.contains(":")) {
      // no parsing necessary
      return permalink;
    }

    Matcher matcher;

    String filename = (String) pageVariables.get(CustomSiteVariables.DUMBO_FILENAME);
    String filenameSlug;
    Date date;

    if (filename != null) {
      matcher = PAT_DATE_FILENAME.matcher(filename);
      String filenameRest;
      if (matcher.find()) {
        date = SDF_YMD.get().parse(matcher.group(1));
        filenameRest = matcher.group(2);
      } else {
        date = null;
        filenameRest = filename;
      }

      @SuppressWarnings("unused")
      String filenameSlugSuffix;
      matcher = PAT_FILENAME_SLUG.matcher(filenameRest);
      if (matcher.find()) {
        filenameSlug = matcher.group(1);
        filenameSlugSuffix = matcher.group(2);
      } else {
        filenameSlug = filenameRest;
        filenameSlugSuffix = "";
      }
      filenameSlug = filenameSlug.toLowerCase(Locale.ENGLISH);
      filenameSlug = PAT_ALPHANUM_LOWER.matcher(filenameSlug).replaceAll(" ").trim().replace(' ',
          '-').replaceAll("\\-\\-+", "-");
    } else {
      filenameSlug = null;
      date = null;
    }

    matcher = PAT_PERMA_VAR.matcher(permalink);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {

      String key = matcher.group(1);
      String value = null;
      switch (key) {
        case "title":
          if (filenameSlug == null) {
            throw new IllegalStateException("filename not specified");
          }
          value = permalinkTitle(pageVariables, filenameSlug);
          break;
        case "name":
          String name = (String) pageVariables.get("name");
          if (name == null) {
            throw new IllegalStateException("Name unknown for " + permalink);
          }
          value = name;
          break;
        default:
          SimpleDateFormat sdf = DATE_KEYS.get(key);
          if (sdf != null) {
            if (date != null) {
              value = sdf.format(date);
            } else {
              throw new IllegalStateException("Date unknown for " + filename
                  + " but permalink needs one: " + permalink);
            }
          }
          break;
      }

      if (value == null) {
        System.err.println("Unknown permalink key: " + key);
        value = ":" + key;
        throw new UnsupportedOperationException("Unknown permalink key: " + key);
      }

      matcher.appendReplacement(sb, value);
    }
    matcher.appendTail(sb);

    String s = sb.toString().replaceAll("//+", "/");
    pageVariables.put("permalink", s);

    return s;
  }

  private static String permalinkTitle(Map<String, Object> pageVariables, String filenameSlug) {
    String slug = (String) pageVariables.get("slug");
    if (slug == null) {
      slug = filenameSlug;
    }
    return slug;
  }
}
