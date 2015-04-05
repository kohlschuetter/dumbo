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

package org.jabsorb.serializer.response;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.ProcessedObject;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.jabsorb.serializer.response.fixups.FixupProcessedObject;
import org.jabsorb.serializer.response.results.SuccessfulResult;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Serializer State which can handle neither circular references or duplicates.
 * An exception is thrown when circular references are found and duplicates are
 * ignored.
 * 
 * @author William Becker
 */
public class NoCircRefsOrDupes implements SerializerState,
    CircularReferenceHandler, DuplicateReferenceHandler
{
  /**
   * Represents the current json location that we are at during processing. Each
   * time we go one layer deeper in processing, the reference is pushed onto the
   * stack And each time we recurse out of that layer, it is popped off the
   * stack.
   */
  protected final LinkedList<Object> currentLocation;

  /**
   * The key is the processed object. The value is a ProcessedObject instance
   * which contains both the object that was processed, and other information
   * about the object used for generating fixups when marshalling.
   */
  private final Map<Object, FixupProcessedObject> processedObjects;

  /**
   * Creates a new NoCircRefsOrDupes
   */
  public NoCircRefsOrDupes()
  {
    processedObjects = new IdentityHashMap<Object, FixupProcessedObject>();
    currentLocation = new LinkedList<Object>();
  }

  public Object checkObject(Object parent, Object java, Object ref)
      throws MarshallException
  {
    {
      // check for duplicate objects or circular references
      FixupProcessedObject p = this.getProcessedObject(java);
      final Object returnValue;

      // if this object hasn't been seen before, mark it as seen and continue forth
      if (p != null)
      {
        //TODO: make test cases to explicitly handle all 4 combinations of the 2 option
        //settings (both on the client and server)

        // handle throwing of circular reference exception and/or serializing duplicates, depending
        // on the options set in the serializer!
        final boolean foundCircRef = this.isAncestor(p, parent);
        if (foundCircRef)
        {
          returnValue = this.circularReferenceFound(p.getLocation(), ref, java);
        }
        else
        {
          returnValue = this.duplicateFound(p.getLocation(), ref, java);
        }
      }
      else
      {
        returnValue = null;
      }
      if (returnValue == null)
      {
        this.push(parent, java, ref);
      }
      return returnValue;
    }
  }

  public Object circularReferenceFound(List<Object> originalLocation,
      Object ref, Object java) throws MarshallException
  {
    throw new MarshallException("Circular Reference");
  }

  public JSONObject createObject(String key, Object json) throws JSONException
  {
    final JSONObject toReturn = new JSONObject();
    toReturn.put(key, json);
    return toReturn;
  }

  public SuccessfulResult createResult(Object requestId, Object json)
  {
    return new SuccessfulResult(requestId, json);
  }

  public Object duplicateFound(List<Object> originalLocation, Object ref,
      Object java) throws MarshallException
  {
    return null;
  }

  public FixupProcessedObject getProcessedObject(Object object)
  {
    // get unique key for this object
    // this is the basis for determining if we have already processed the object or not.
    return processedObjects.get(object);
  }

  public void pop() throws MarshallException
  {
    if (currentLocation.size() == 0)
    {
      // this is a sanity check
      throw new MarshallException(
          "scope error, attempt to pop too much off the scope stack.");
    }
    currentLocation.removeLast();
  }

  public Object push(Object parent, Object obj, Object ref)
  {
    FixupProcessedObject parentProcessedObject = null;

    if (parent != null)
    {
      parentProcessedObject = getProcessedObject(parent);

      if (parentProcessedObject == null)
      {
        // this is a sanity check-- it should never occur
        throw new IllegalArgumentException(
            "attempt to process an object with an unprocessed parent");
      }
    }

    FixupProcessedObject p = new FixupProcessedObject(obj,
        parentProcessedObject);

    processedObjects.put(obj, p);
    if (ref != null)
    {
      p.setRef(ref);
      currentLocation.add(ref);
    }
    return obj;
  }

  public void setMarshalled(Object marshalledObject, Object java)
  {
    //Nothing to do
  }

  public void setSerialized(Object source, Object target)
      throws UnmarshallException
  {
    if (source == null)
    {
      throw new UnmarshallException("source object may not be null");
    }
    FixupProcessedObject p = getProcessedObject(source);
    if (p == null)
    {
      // this should normally never happen- it's a sanity check.
      throw new UnmarshallException(
          "source object must be already registered as a ProcessedObject "
              + source);
    }
    p.setSerialized(target);
  }

  public void store(Object obj)
  {
    FixupProcessedObject p = new FixupProcessedObject(obj, null);
    processedObjects.put(obj, p);
  }

  /**
   * Determine if a duplicate child object of the given parentis a circular
   * reference with the given ProcessedObject. We know it's a circular reference
   * if we can walk up the parent chain and find the ProcessedObject. If instead
   * we find null, then it's a duplicate instead of a circular ref.
   * 
   * @param dup the duplicate object that might also be the original reference
   *          in a circular reference.
   * @param parent the parent of an object that might be a circular reference.
   * @return true if the duplicate is a circular reference or false if it's a
   *         duplicate only.
   */
  protected boolean isAncestor(ProcessedObject dup, Object parent)
  {
    // walk up the ancestry chain until we either find the duplicate
    // (which would mean it's a circular ref)
    // or we find null (the end of the chain) which would mean it's a duplicate only.
    FixupProcessedObject ancestor = getProcessedObject(parent);
    while (ancestor != null)
    {
      if (dup == ancestor)
      {
        return true;
      }
      ancestor = ancestor.getParent();
    }
    return false;
  }
}
