/*
 * Copyright 2022,2023 Christian Kohlschütter
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
package com.kohlschutter.dumbo.placeholders;

import com.kohlschutter.dumbo.annotations.CSSResource;
import com.kohlschutter.dumbo.annotations.JavaScriptResource;
import com.kohlschutter.dumbo.api.DumboComponent;
import com.kohlschutter.dumbo.jquery.JQuerySupport;

/**
 * Improves handling of placeholder resources
 */
@CSSResource(value = "css/placeholders.css", group = "dumbo")
@JavaScriptResource(value = "js/placeholders.js", group = "dumbo", async = true)
public interface PlaceholdersExtension extends DumboComponent, JQuerySupport {
}
