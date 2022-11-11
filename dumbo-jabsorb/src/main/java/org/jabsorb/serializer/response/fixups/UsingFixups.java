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
package org.jabsorb.serializer.response.fixups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.response.FixUp;
import org.jabsorb.serializer.response.NoCircRefsOrDupes;
import org.jabsorb.serializer.response.results.SuccessfulResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is the superclass of all "fixup" generating circular reference / duplicate object resolvers.
 * 
 * @author William Becker
 */
public abstract class UsingFixups extends NoCircRefsOrDupes {
  /**
   * The list of class types that are considered primitives that should not be fixed up when
   * fixupDuplicatePrimitives is false.
   */
  private static Class<?>[] duplicatePrimitiveTypes = {
      String.class, Integer.class, Boolean.class, Long.class, Byte.class, Double.class, Float.class,
      Short.class};

  /**
   * Adds fixups to a JSONObject
   * 
   * @param o The object to which the fixups are to be added.
   * @param fixUps The fixups to add.
   * @return The object to which the fixups have been added.
   * @throws JSONException If an exception occurs when the fixups are created.
   */
  public static JSONObject addFixups(JSONObject o, Collection<FixUp> fixUps) throws JSONException {
    if (fixUps != null && fixUps.size() > 0) {
      JSONArray fixups = new JSONArray();
      for (FixUp fixup : fixUps) {
        fixups.put(fixup.toJSONArray());
      }
      o.put(FixUp.FIXUPS_FIELD, fixups);
    }
    return o;
  }

  /**
   * Determine if this serializer considers the given Object to be a primitive wrapper type Object.
   * This is used to determine which types of Objects should be fixed up as duplicates if the
   * fixupDuplicatePrimitives flag is false.
   * 
   * @param o Object to test for primitive.
   * @return True if the object is a primi
   */
  protected static boolean isPrimitive(Object o) {
    if (o == null) {
      return true; // extra safety check- null is considered primitive too
    }

    Class<?> c = o.getClass();

    for (int i = 0, j = duplicatePrimitiveTypes.length; i < j; i++) {
      if (duplicatePrimitiveTypes[i] == c) {
        return true;
      }
    }
    return false;
  }

  /**
   * A List of FixUp objects that are generated during processing for circular references and/or
   * duplicate references.
   */
  private final Collection<FixUp> fixups = new ArrayList<FixUp>();

  @Override
  public JSONObject createObject(String key, Object json) throws JSONException {
    final JSONObject toReturn = super.createObject(key, json);
    UsingFixups.addFixups(toReturn, this.fixups);
    return toReturn;
  }

  @Override
  public SuccessfulResult createResult(Object requestId, Object json) {
    return new FixupsResult(requestId, json, fixups);
  }

  /**
   * Adds a fixup to the list of known fixups.
   * 
   * @param originalLocation The location of the original version of the object.
   * @param ref The reference by which the current object is denoted.
   * @return The object to put in the place of the current object.
   */
  protected Object addFixUp(List<Object> originalLocation, Object ref) {
    currentLocation.add(ref);
    fixups.add(new FixUp(currentLocation, originalLocation));
    try {
      pop();
    } catch (MarshallException me) {
      // This cannot happen as it currentLocation.add() ensures that pop will
      // not be called on an empty currentLocation
    }
    return JSONObject.NULL;
  }

}
