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
package com.kohlschutter.dumbo.markdown.liqp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import liqp.TemplateContext;
import liqp.filters.Filter;

public class DateToXmlschemaFilter extends Filter {
  private static final liqp.filters.Date LIQP_DATE_FILTER = new liqp.filters.Date() {
  };
  private static final SimpleDateFormat[] PARSERS = new SimpleDateFormat[] {
      new SimpleDateFormat("yyyy-MM-dd hh:mm:ss zzz", Locale.ENGLISH), new SimpleDateFormat(
          "yyyy-MM-dd hh:mm zzz", Locale.ENGLISH)};
  private static final SimpleDateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ",
      Locale.ENGLISH);

  public DateToXmlschemaFilter() {
    super("date_to_xmlschema");
  }

  @Override
  public Object apply(Object value, TemplateContext context, Object... params) {
    String date = LIQP_DATE_FILTER.apply(value, context, new Object[0]).toString();

    Date d = null;
    for (SimpleDateFormat parser : PARSERS) {
      try {
        d = parser.parse(date);
        break;
      } catch (ParseException e) {
        continue;
      }
    }
    if (d == null) {
      throw new IllegalStateException("Cannot parse date: " + date);
    }

    return ISO_8601.format(d);
  }
}
