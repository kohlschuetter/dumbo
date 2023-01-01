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
package com.kohlschutter.dumbo.markdown;

import com.kohlschutter.dumbo.ConsoleSupport;
import com.kohlschutter.dumbo.annotations.ServletMapping;
import com.kohlschutter.dumbo.annotations.Servlets;
import com.kohlschutter.dumbo.api.DumboComponent;
import com.kohlschutter.dumbo.appdefaults.AppDefaultsSupport;
import com.kohlschutter.dumbo.bootstrap.BootstrapSupport;

@Servlets({ //
    @ServletMapping(map = "*.html", to = HtmlJspServlet.class),
    @ServletMapping(map = "*.md", to = MarkdownServlet.class),
    //
})
public interface DumboMarkdownSupport extends DumboComponent, AppDefaultsSupport, BootstrapSupport,
    ConsoleSupport {

}
