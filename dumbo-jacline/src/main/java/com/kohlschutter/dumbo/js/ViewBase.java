/*
 * dumbo-jacline
 *
 * Copyright 2023 Christian Kohlschütter
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
package com.kohlschutter.dumbo.js;

import java.util.Objects;

import elemental2.dom.Element;
import elemental2.dom.Node;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
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
