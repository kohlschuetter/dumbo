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

package org.jabsorb.serializer;

import org.jabsorb.serializer.response.results.SuccessfulResult;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used by Serializers to hold state during marshalling and unmarshalling. It
 * keeps track of all Objects encountered during processing for the purpose of
 * detecting circular references and/or duplicates.
 */
public interface SerializerState
{
  /**
   * Checks whether the current object being parsed needs action taken upon it
   * or is ok to marshal. If any value is returned except null, it means that it
   * should not be marshalled.
   * 
   * @param parent The parent of the current object
   * @param currentObject The current object being marshalled
   * @param ref additional reference (String|Integer) to add on to the scope's
   *          current location.
   * @return The value to add to the json object being created, or null if the
   *         value to be added should be created by marshalling the current
   *         object.
   * @throws MarshallException if a scope error occurs (this won't normally
   *           occur.
   */
  public Object checkObject(Object parent, Object currentObject, Object ref)
      throws MarshallException;

  /**
   * Creates a json object with the data in <code>json</code> stored at
   * <code>key</code>
   * 
   * @param key The key in which the data should be stored
   * @param json The data to store
   * @return A new JSONObject with the data stored within it.
   * @throws JSONException If an exception occurs storing the data.
   */
  public JSONObject createObject(String key, Object json) throws JSONException;

  /**
   * Creates a result to be returned to the jabsorb client.
   * 
   * @param requestId The id of the request for which this result is to be mad e
   * @param json The serialized object
   * @return Some kind of SuccessfulResult
   */
  public SuccessfulResult createResult(Object requestId, Object json);

  /**
   * If the given object has already been processed, return the ProcessedObject
   * wrapper for that object which will indicate the original location from
   * where that Object was processed from.
   * 
   * @param object Object to check.
   * @return ProcessedObject wrapper for the given object or null if the object
   *         hasn't been processed yet in this SerializerState.
   */
  public ProcessedObject getProcessedObject(Object object);

  /**
   * Pop off one level from the scope stack of the current location during
   * processing. If we are already at the lowest level of scope, then this has
   * no action.
   * 
   * @throws MarshallException If called when currentLocation is empty
   */
  public void pop() throws MarshallException;

  /**
   * Record the given object as a ProcessedObject and push into onto the scope
   * stack. This is only used for marshalling. The store method should be used
   * for unmarshalling.
   * 
   * @param parent parent of object to process. Can be null if it's the root
   *          object being processed. it should be an object that was already
   *          processed via a previous call to processObject.
   * @param obj object being processed
   * @param ref reference to object within parent-- should be a String if parent
   *          is an object, and Integer if parent is an array. Can be null if
   *          this is the root object that is being pushed/processed.
   * @return The object that was pushed
   */
  public Object push(Object parent, Object obj, Object ref);

  /**
   * Tells the serializer state that marshalling for the given object has been
   * completed
   * 
   * @param marshalledObject What the object was marshalled into
   * @param java The object that was marshalled
   */
  public void setMarshalled(Object marshalledObject, Object java);

  /**
   * Associate the incoming source object being serialized to it's serialized
   * representation. Currently only used within tryUnmarshall and unmarshall.
   * This MUST be called before a given unmarshall or tryUnmarshall recurses
   * into child objects to unmarshall them. The purpose is to stop the recursion
   * that can take place when circular references/duplicates are in the input
   * json being unmarshalled.
   * 
   * @param source source object being unmarshalled.
   * @param target target serialized representation of the object that the
   *          source object is being unmarshalled to.
   * @throws UnmarshallException if the source object is null, or is not already
   *           stored within a ProcessedObject.
   */
  public void setSerialized(Object source, Object target)
      throws UnmarshallException;

  /**
   * Much simpler version of push to just account for the fact that an object
   * has been processed (used for unmarshalling where we just need to re-hook up
   * circ refs and duplicates and not generate fixups.)
   * 
   * @param obj Object to account for as being processed.
   */
  public void store(Object obj);
}
