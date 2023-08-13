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
package com.kohlschutter.dumbo;

import com.kohlschutter.stringhold.StringHolder;
import com.kohlschutter.stringhold.StringHolderSequence;

/**
 * Provides access to the HTML HEAD/BODY-level definitions of extension resources.
 *
 * Some of the resources may be optional; their inclusion can be controlled via
 * {@link RenderState#setMarkedUsed(Class)}.
 *
 * @author Christian Kohlschütter
 */
public final class ExtensionResourceHelper {
  private ExtensionResourceHelper() {
    throw new IllegalStateException("No instances");
  }

  public static StringHolder htmlHead(final ServerApp app) {
    StringHolderSequence sb = StringHolder.newSequence();
    for (ExtensionImpl ext : app.getExtensions()) {
      sb.append(ext.htmlHead(app));
    }

    return sb;
  }

  public static StringHolder htmlBodyTop(final ServerApp app) {
    StringHolderSequence sb = StringHolder.newSequence();

    for (ExtensionImpl ext : app.getExtensions()) {
      sb.append(ext.htmlBodyTop(app));
    }

    return sb;
  }
}
