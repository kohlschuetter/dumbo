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
package com.kohlschutter.dumbo.annotations;

import jakarta.servlet.Servlet;

/**
 * A component is something that can have annotations of the following types:
 *
 * <ul>
 * <li>{@link Services} — Component exposes a Java/JSON RPC service</li>
 * <li>{@link Servlets} — Component exposes a {@link Servlet}</li>
 * <li>{@link CSSResources} — Component uses a set of {@link CSSResource}s</li>
 * <li>{@link CSSResource} — Component uses the following CSS resources</li>
 * <li>{@link HTMLResources} — Component uses a set of {@link HTMLResource}s</li>
 * <li>{@link HTMLResource} — Component uses the following HTML resources</li>
 * <li>{@link JavaScriptResources} — Component uses a set of {@link JavaScriptResource}s</li>
 * <li>{@link JavaScriptResource} — Component uses the following JavaScript resources</li>
 * </ul>
 *
 * @author Christian Kohlschütter
 */
public interface Component {
}
