/*
 * Copyright 2022-2025 Christian Kohlschütter
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
package com.kohlschutter.dumbo.appdefaults;

import com.kohlschutter.dumbo.annotations.CSSResource;
import com.kohlschutter.dumbo.annotations.JavaScriptResource;
import com.kohlschutter.dumbo.api.DumboComponent;

/**
 * Simplifies app development by automatically providing references to the app's default JavaScript
 * and CSS files.
 */
@JavaScriptResource(value = "js/app.js", optional = true)
@CSSResource(value = "css/default.css", optional = true)
public interface AppDefaultsSupport extends DumboComponent {

}
