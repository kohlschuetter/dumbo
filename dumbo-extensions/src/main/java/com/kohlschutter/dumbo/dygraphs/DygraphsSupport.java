/**
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
package com.kohlschutter.dumbo.dygraphs;

import com.kohlschutter.dumbo.Extension;

/*
 * Helper class to add dygraphs support to the demo server.
 * 
 * @see dumbo-helloworld
 * @see http://dygraphs.com/
 */
public final class DygraphsSupport extends Extension {
  @Override
  protected void initResources() {
    registerJavaScript("js/dygraph-combined.js");
    registerJavaScript("js/dygraph-extras.js");
  }
}
