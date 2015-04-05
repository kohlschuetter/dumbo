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

package org.jabsorb.serializer.request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parent class for handing circular references in different ways.
 */
public abstract class RequestParser
{
  /**
   * Unmarshalls an value from a JSONObject stored under a specific key.
   * @param object The object which contains the value to unmarshall.
   * @param key The key under which the object stores the value to unmarshall.
   * @return An unmarshalled Object
   * @throws JSONException if an exception occurs while parsing.
   */
  public Object unmarshall(final JSONObject object, final String key)
      throws JSONException
  {
    Object value = object.get(key);
    if (value instanceof JSONObject)
    {
      return this.unmarshallObject(object, key);
    }
    else if (value instanceof JSONArray)
    {
      return this.unmarshallArray(object, key);
    }
    return value;
  }

  /**
   * Unmarshalls an array from a JSONObject
   * 
   * @param object The object to unmarshall
   * @param key The key in which the array exists within the JSONObject
   * @return An array of values which may contian circular references. Warning:
   *         do not call toString on this object!
   * @throws JSONException If an exception occurs parsing jsonReq.
   */
  public abstract JSONArray unmarshallArray(final JSONObject object,
      final String key) throws JSONException;

  /**
   * Unmarshalls an inner JSONObject from within another JSONObject
   * 
   * @param object The request from the client may contain circular references
   * @param key The key in which the object exists within the JSONObject
   * @return An object which may contian circular references. Warning: do not
   *         call toString on this object!
   * @throws JSONException If an exception occurs parsing object.
   */
  public abstract JSONObject unmarshallObject(final JSONObject object,
      final String key) throws JSONException;
}
