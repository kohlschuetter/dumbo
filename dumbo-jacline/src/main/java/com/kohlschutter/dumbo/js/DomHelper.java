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

import com.kohlschutter.jacline.lib.log.CommonLog;

import elemental2.dom.DOMTokenList;
import elemental2.dom.DomGlobal;
import elemental2.dom.Element;

@SuppressWarnings("PMD.GuardLogStatement")
public final class DomHelper {
  private DomHelper() {
  }

  public static void defer(Runnable op) {
    defer(0, op);
  }

  public static void defer(long timeout, Runnable op) {
    DomGlobal.setTimeout((o) -> op.run(), timeout);
  }

  public static void toggleClass(Element elem, boolean on, String... classNames) {
    if (elem == null) {
      CommonLog.warn("toggleClass for null elem", (Object[]) classNames);
      return;
    }
    DOMTokenList classList = elem.classList;
    if (on) {
      classList.add(classNames);
    } else {
      classList.remove(classNames);
    }
  }

  public static void addClass(Element elem, String... classNames) {
    if (elem == null) {
      CommonLog.warn("addClass for null elem", (Object[]) classNames);
      return;
    }
    elem.classList.add(classNames);
  }

  public static void removeClass(Element elem, String... classNames) {
    if (elem == null) {
      CommonLog.warn("removeClass for null elem", (Object[]) classNames);
      return;
    }
    elem.classList.remove(classNames);
  }

  /**
   * Finds the element bearing the given className, referring to the given element (which usually
   * means that the element-to-be-found contains an attribute named "for" that contains the id of
   * the reference element — or, as a fallback if no id is given, checks if the next sibling has
   * that class).
   *
   * @param className The class name.
   * @param referenceElement The reference element.
   * @return The element, or {@code null}.
   */
  public static Element findElementWithClassForElement(String className, Element referenceElement) {
    String id = referenceElement.id;
    if (id == null) {
      Element sibl = referenceElement.nextElementSibling;
      if (sibl != null && sibl.classList.contains(className)) {
        return sibl;
      } else {
        return null;
      }
    } else {
      // FIXME quote ID?
      return DomGlobal.document.querySelector("." + className + "[for='" + id + "']");
    }
  }
}
