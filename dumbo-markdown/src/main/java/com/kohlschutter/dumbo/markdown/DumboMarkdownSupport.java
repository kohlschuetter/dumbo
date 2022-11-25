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
public class DumboMarkdownSupport extends Extension {

  @Override
  protected void initResources() {

  }
}
