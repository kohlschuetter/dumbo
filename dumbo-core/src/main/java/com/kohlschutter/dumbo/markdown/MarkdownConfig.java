package com.kohlschutter.dumbo.markdown;

import liqp.ParseSettings;
import liqp.TemplateParser;
import liqp.parser.Flavor;

public class MarkdownConfig {
  public static final TemplateParser LIQP_PARSER = new TemplateParser.Builder().withParseSettings(
      new ParseSettings.Builder().with(Flavor.JEKYLL.defaultParseSettings()).with(
          new DumboInclude()).build()).build();
}
