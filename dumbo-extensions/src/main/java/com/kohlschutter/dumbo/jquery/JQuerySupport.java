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
package com.kohlschutter.dumbo.jquery;

import com.kohlschutter.dumbo.annotations.JavaScriptResource;
import com.kohlschutter.dumbo.annotations.JavaScriptResources;
import com.kohlschutter.dumbo.api.DumboComponent;

/**
 * Helper class to add Bootstrap.js support to the demo server.
 *
 * See {@code dumbo-helloworld}.
 */
@JavaScriptResources({//
    @JavaScriptResource({"js/jquery-3.3.1.slim.min.js"}) //
})
public interface JQuerySupport extends DumboComponent {

}
