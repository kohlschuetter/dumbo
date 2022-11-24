package com.kohlschutter.dumbo.jek.liqp.filters;

import liqp.TemplateContext;
import liqp.filters.Filter;

public class NumberOfWords extends Filter {
  public NumberOfWords() {
    super("number_of_words");
  }

  @Override
  public Object apply(Object value, TemplateContext context, Object... params) {
    String content = asString(value, context).trim();

    if (content.isEmpty()) {
      return 0;
    }

    String param = params.length == 0 ? "" : asString(params[0], context);
    // FIXME param, CJK

    return content.split("[ ]+").length;
  }
}
