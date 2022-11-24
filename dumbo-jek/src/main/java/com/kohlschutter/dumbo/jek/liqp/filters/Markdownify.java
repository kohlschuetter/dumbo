package com.kohlschutter.dumbo.jek.liqp.filters;

import liqp.TemplateContext;
import liqp.filters.Filter;

public class Markdownify extends Filter {

  @Override
  public Object apply(Object value, TemplateContext context, Object... params) {
    String content = super.asString(value, context);

    if (content.isEmpty()) {
        return content;
      }

    System.out.println("FIXME MARKDOWNIFY: " + content);

    return content;
  }
}
