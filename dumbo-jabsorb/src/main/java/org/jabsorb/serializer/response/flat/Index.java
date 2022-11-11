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
package org.jabsorb.serializer.response.flat;

/**
 * Allows for late allocation of an object's index
 * 
 * @author William Becker
 */
class Index {
  /**
   * The index of the object
   */
  private String index;

  /**
   * Creates a new index. The value will be set later.
   */
  public Index() {
    this(null);
  }

  /**
   * Creates a new index
   * 
   * @param index The index of the object
   */
  public Index(String index) {
    this.index = index;
  }

  /**
   * Gets the index
   * 
   * @return The index
   */
  public String getIndex() {
    return index;
  }

  /**
   * Sets the index
   * 
   * @param index The value to set
   */
  public void setIndex(String index) {
    this.index = index;
  }

  @Override
  public String toString() {
    return index;
  }
}
