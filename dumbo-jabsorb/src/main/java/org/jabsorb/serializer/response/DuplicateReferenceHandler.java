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

import java.util.List;

import org.jabsorb.serializer.MarshallException;

/**
 * Allows duplicate references to be signalled when found in Java code.
 * 
 * @author William Becker
 */
public interface DuplicateReferenceHandler
{
  /**
   * Signals that a duplicate reference was found.
   * 
   * @param originalLocation The location where it first appeared
   * @param ref The reference of from the current location where it next
   *          appeared
   * @param java The object which appears twice
   * @return The object to put in the place of the duplicate reference in the
   *         JSONObject
   * @throws MarshallException Thrown if the given object cannot be marshalled
   */
  public Object duplicateFound(List<Object> originalLocation,
      Object ref, Object java) throws MarshallException;

}
