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
package com.kohlschutter.dumbo.disableoverscroll;

import com.kohlschutter.dumbo.annotations.JavaScriptResource;
import com.kohlschutter.dumbo.annotations.JavaScriptResources;
import com.kohlschutter.dumbo.api.DumboComponent;

/**
 * Disable the "rubber band" / "overscrolling" annoyance in Google Chrome on OS X when scrolling
 * over the top of a page (e.g., two-finger touch-and-drag downwards).
 *
 * For improved smoothness, you may want to add the CSS style {@code overflow: hidden;} to the
 * {@code BODY} tag.
 */
@JavaScriptResources(@JavaScriptResource(value = "js/disableoverscroll.js", async = true))
public interface DisableOverScrollFeature extends DumboComponent {

}
