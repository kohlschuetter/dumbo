package com.kohlschutter.dumbo.jek;

import com.kohlschutter.dumbo.Extension;
import com.kohlschutter.dumbo.Extensions;
import com.kohlschutter.dumbo.ServletMapping;
import com.kohlschutter.dumbo.Servlets;
import com.kohlschutter.dumbo.bootstrap.BootstrapSupport;
import com.kohlschutter.dumbo.console.ConsoleSupport;
import com.kohlschutter.dumbo.ext.AppDefaultsSupport;

@Servlets({ //
    @ServletMapping(map = "*.html", to = HtmlJspServlet.class),
    @ServletMapping(map = "*.md", to = MarkdownServlet.class),
    //
})
@Extensions({AppDefaultsSupport.class, BootstrapSupport.class, ConsoleSupport.class})
public class DumboJekSupport extends Extension {

  @Override
  protected void initResources() {

  }
}
