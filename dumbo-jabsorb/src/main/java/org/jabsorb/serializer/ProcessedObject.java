/*
 * jabsorb - a Java to JavaScript Advanced Object Request Broker
 * http://www.jabsorb.org
 *
 * Copyright 2007-2009 The jabsorb team
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

/**
 * Represents an object that has been already processed by the JSONSerializer
 * this is stored in the SerializerState in order to detect circular references
 * and duplicates.
 */
public class ProcessedObject
{
  /**
   * The processed incoming object. When marshalling, this is the java object
   * that is being marshalled to json, when unmarshalling, this is the json
   * object being marshalled to java.
   */
  private final Object object;

  /**
   * Creates a new ProcessedObject
   * 
   * @param object The processed incoming object. When marshalling, this is the
   *          java object that is being marshalled to json, when unmarshalling,
   *          this is the json object being marshalled to java.
   */
  public ProcessedObject(Object object)
  {
    this.object = object;
  }

  /**
   * The serialized equivalent of the object. Only used for unmarshalling to
   * recreate circular reference equivalences in java from fixed up incoming
   * json. TODO: should this just be called unmarshalled? it's only used in the
   * unmarshall case at this point but it might be useful later in the marshall
   * case as well...
   */
  private Object serialized;

  /**
   * Set the serialized java Object that this ProcessedObject represents. Only
   * used when unmarshalling, to re-connect circular references/duplicates that
   * were fixed up.
   * 
   * @param serialized java Object that the json object represented by this
   *          ProcessedObject is being serialized to.
   */
  public void setSerialized(Object serialized)
  {
    this.serialized = serialized;
  }

  /**
   * Get the serialized java Object that this ProcessedObject represents. Only
   * used when unmarshalling, to re-connect circular references/duplicates that
   * were fixed up.
   * 
   * @return java Object that the json object represented by this
   *         ProcessedObject is being serialized to.
   */
  public Object getSerialized()
  {
    return serialized;
  }

  /**
   * Get the actual Object that this ProcessedObject wraps.
   * 
   * @return the actual Object that this ProcessedObject wraps.
   */
  public Object getObject()
  {
    return object;
  }
}
