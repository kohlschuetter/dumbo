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
 *
 */

package org.jabsorb.serializer.request.fixups;

import org.jabsorb.serializer.request.RequestParser;
import org.jabsorb.serializer.response.FixUp;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Circular references are handled by generating an extra parameter, fixups,
 * which contains an array of the paths to get to the duplicate value, matched
 * with the path to get to the value that this duplicates.
 */
public class FixupsCircularReferenceHandler extends RequestParser
{
  @Override
  public JSONArray unmarshallArray(final JSONObject jsonReq, final String key)
      throws JSONException
  {
    return (JSONArray) _unmarshall(jsonReq, key);
  }

  @Override
  public JSONObject unmarshallObject(JSONObject jsonReq, String key)
      throws JSONException
  {
    return (JSONObject) _unmarshall(jsonReq, key);
  }

  /**
   * Applies fixups to an JSONObject or JSONArray
   * 
   * @param data The object which holds the Object/array and the fixup info
   * @param key The Key in data for the object/array
   * @return The Object/Array
   * @throws JSONException If the json cannot be read
   */
  private Object _unmarshall(JSONObject data, String key) throws JSONException
  {
    // TODO: handle error field here
    final Object arguments = data.get(key);
    final JSONArray fixups = data.optJSONArray(FixUp.FIXUPS_FIELD);

    // apply the fixups (if any) to the parameters. This will result
    // in a JSONArray that might have circular references-- so
    // the toString method (or anything that internally tries to traverse
    // the JSON (without being aware of this) should not be called after this
    // point

    if (fixups != null)
    {
      for (int i = 0; i < fixups.length(); i++)
      {
        JSONArray assignment = fixups.getJSONArray(i);
        JSONArray fixup = assignment.getJSONArray(0);
        JSONArray original = assignment.getJSONArray(1);
        applyFixup(arguments, fixup, original);
      }
    }
    return arguments;
  }

  /**
   * Apply one fixup assigment to the incoming json arguments. WARNING: the
   * resultant "fixed up" arguments may contain circular references after this
   * operation. That is the whole point of course-- but the JSONArray and
   * JSONObject's themselves aren't aware of circular references when certain
   * methods are called (e.g. toString) so be careful when handling these
   * circular referenced json objects.
   * 
   * @param toFix the element to apply the fixup to.
   * @param fixup the fixup entry.
   * @param original the original value to assign to the fixup.
   * @throws org.json.JSONException if invalid or unexpected fixup data is
   *           encountered.
   */
  private void applyFixup(Object toFix, JSONArray fixup, JSONArray original)
      throws JSONException
  {
    int last = fixup.length() - 1;

    if (last < 0)
    {
      throw new JSONException("fixup path must contain at least 1 reference");
    }

    Object originalObject = traverse(toFix, original, false);
    Object fixupParent = traverse(toFix, fixup, true);

    // the last ref in the fixup needs to be created
    // it will be either a string or number depending on if the fixupParent is a
    // JSONObject or JSONArray

    if (fixupParent instanceof JSONObject)
    {
      String objRef = fixup.optString(last, null);
      if (objRef == null)
      {
        throw new JSONException("last fixup reference not a string");
      }
      ((JSONObject) fixupParent).put(objRef, originalObject);
    }
    else
    {
      int arrRef = fixup.optInt(last, -1);
      if (arrRef == -1)
      {
        throw new JSONException("last fixup reference not a valid array index");
      }
      ((JSONArray) fixupParent).put(arrRef, originalObject);
    }
  }

  /**
   * Given a previous json object, find the next object under the given index.
   * 
   * @param prev object to find subobject of.
   * @param idx index of sub object to find.
   * @return the next object in a fixup reference chain (prev[idx])
   * @throws JSONException if something goes wrong.
   */
  private Object next(Object prev, int idx) throws JSONException
  {
    if (prev == null)
    {
      throw new JSONException("cannot traverse- missing object encountered");
    }

    if (prev instanceof JSONArray)
    {
      return ((JSONArray) prev).get(idx);
    }
    throw new JSONException("not an array");
  }

  /**
   * Given a previous json object, find the next object under the given ref.
   * 
   * @param prev object to find subobject of.
   * @param ref reference of sub object to find.
   * @return the next object in a fixup reference chain (prev[ref])
   * @throws JSONException if something goes wrong.
   */
  private Object next(Object prev, String ref) throws JSONException
  {
    if (prev == null)
    {
      throw new JSONException("cannot traverse- missing object encountered");
    }
    if (prev instanceof JSONObject)
    {
      return ((JSONObject) prev).get(ref);
    }
    throw new JSONException("not an object");
  }

  /**
   * Traverse a list of references to find the target reference in an original
   * or fixup list.
   * 
   * @param origin origin JSONArray (arguments) to begin traversing at.
   * @param refs JSONArray containing array integer references and or String
   *          object references.
   * @param fixup if true, stop one short of the traversal chain to return the
   *          parent of the fixup rather than the fixup itself (which will be
   *          non-existant)
   * @return either a JSONObject or JSONArray for the Object found at the end of
   *         the traversal.
   * @throws JSONException if something unexpected is found in the data
   */
  private Object traverse(Object origin, JSONArray refs, boolean fixup)
      throws JSONException
  {
    try
    {
      JSONArray arr = null;
      JSONObject obj = null;
      if (origin instanceof JSONArray)
      {
        arr = (JSONArray) origin;
      }
      else
      {
        obj = (JSONObject) origin;
      }

      // where to stop when traversing
      int stop = refs.length();

      // if looking for the fixup, stop short by one to find the parent of the
      // fixup instead.
      // because the fixup won't exist yet and needs to be created
      if (fixup)
      {
        stop--;
      }

      // find the target object by traversing the list of references
      for (int i = 0; i < stop; i++)
      {
        Object next;
        if (arr == null)
        {
          next = next(obj, refs.optString(i, null));
        }
        else
        {
          next = next(arr, refs.optInt(i, -1));
        }
        if (next instanceof JSONObject)
        {
          obj = (JSONObject) next;
          arr = null;
        }
        else
        {
          obj = null;
          arr = (JSONArray) next;
        }
      }
      if (arr == null)
      {
        return obj;
      }
      return arr;
    }
    catch (Exception e)
    {
      throw new JSONException("unexpected exception");
    }
  }

}
