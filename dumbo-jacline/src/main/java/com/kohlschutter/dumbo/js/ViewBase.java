package com.kohlschutter.dumbo.js;

import java.util.Objects;

import elemental2.dom.Element;
import elemental2.dom.Node;

public abstract class ViewBase {
  private final Node parent;

  protected ViewBase(Node parentNode) {
    this.parent = Objects.requireNonNull(parentNode, "parentNode");
  }

  protected Element querySelector(String selectors) {
    return parent.querySelector(selectors);
  }

  protected Node getParentNode() {
    return parent;
  }

  protected Element querySelector(Element ref, String selectors) {
    if (ref == null) {
      return null;
    }
    return ref.querySelector(selectors);
  }
}
