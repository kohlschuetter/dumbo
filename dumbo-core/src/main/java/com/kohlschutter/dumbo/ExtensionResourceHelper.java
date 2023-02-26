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
    StringHolderSequence sb = new StringHolderSequence();
    for (ExtensionImpl ext : app.getExtensions()) {
      sb.append(ext.htmlHead(app));
    }

    return sb;
  }

  public static StringHolder htmlBodyTop(final ServerApp app) {
    StringHolderSequence sb = new StringHolderSequence();

    for (ExtensionImpl ext : app.getExtensions()) {
      sb.append(ext.htmlBodyTop(app));
    }

    return sb;
  }
}
