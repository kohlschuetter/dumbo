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
      new SimpleDateFormat("yyyy-MM-dd hh:mm:ss zzz"), new SimpleDateFormat(
          "yyyy-MM-dd hh:mm zzz")};
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
