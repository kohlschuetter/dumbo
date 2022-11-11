/*
 * jabsorb - a Java to JavaScript Advanced Object Request Broker
 * http://www.jabsorb.org
 *
 * Copyright 2007-2009 The jabsorb team
 *
 * based on original code from
 * JSON-RPC-Java - a JSON-RPC to Java Bridge with dynamic invocation
 *
 * Copyright Metaparadigm Pte. Ltd. 2004.
 * Michael Clark <michael@metaparadigm.com>
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
package org.jabsorb.serializer.response;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

/**
 * Encapsulates a single fix up entry for a circular reference or duplicate detected during
 * processing of Java into JSON (aka marshalling.)
 */
public class FixUp {
  /**
   * The field in which fixups are held in json messages
   */
  public static final String FIXUPS_FIELD = "fixups";

  /**
   * A List of String|Integer's representing references to locate the place where this fixup entry
   * needs to be created.
   */
  private List<Object> fixupLocation;

  /**
   * List of String|Integer's representing references to locate the original location of the
   * circular reference or duplicate that this FixUp applies to.
   */
  private List<Object> originalLocation;

  /**
   * Create a FixUp for a duplicate or circular reference.
   * 
   * @param fixupLocation a List of String|Integer's representing references to locate the place
   *          where this fixup entry needs to be created.
   * @param originalLocation List of String|Integer's representing references to locate the original
   *          location of the circular reference or duplicate that this FixUp applies to.
   */
  public FixUp(List<Object> fixupLocation, List<Object> originalLocation) {
    this.fixupLocation = new ArrayList<Object>(fixupLocation);
    // pop root object that won't be used in the fixup off the stack
    if (this.fixupLocation.size() > 0) {
      this.fixupLocation.remove(0);
    }
    this.originalLocation = new ArrayList<Object>(originalLocation);
    // pop root object that won't be used in the fixup off the stack
    if (this.originalLocation.size() > 0) {
      this.originalLocation.remove(0);
    }

  }

  /**
   * Convert this FixUp to a JSONArray for transmission over JSON-RPC. The JSONArray will contain
   * two sub JSONArrays, the first one representing the fixup location and the 2nd one representing
   * the original location.
   * 
   * @return the FixUp represented as a JSONArray.
   */
  public JSONArray toJSONArray() {
    JSONArray json = new JSONArray();

    JSONArray fixup = new JSONArray(fixupLocation);
    JSONArray original = new JSONArray(originalLocation);

    json.put(fixup);
    json.put(original);

    return json;
  }
}
