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

package org.jabsorb.test;

import java.io.Serializable;

/**
 * Used for constructor unit tests
 */
public class ConstructorTest implements Serializable
{
  /**
   * Generated Id
   */
  private static final long serialVersionUID = 1L;

  /**
   * Stores the message that is set in the constructor
   */
  private final String message;

  /**
   * A default constructor
   */
  public ConstructorTest()
  {
    message = "default";
  }

  /**
   * Test a boolean constructor
   * 
   * @param b unused
   */
  public ConstructorTest(boolean b)
  {
    message = "boolean";
  }

  /**
   * Test a double constructor
   * 
   * @param l unused
   */
  public ConstructorTest(double l)
  {
    message = "double";
  }

  /**
   * Test a float constructor
   * 
   * @param l unused
   */
  public ConstructorTest(float l)
  {
    message = "float";
  }

  /**
   * Used for testing argument overloading
   * 
   * @param i not used
   */

  public ConstructorTest(int i)
  {
    message = "int";
  }

  /**
   * Used for testing multiple argument overloading
   * 
   * @param i not used
   * @param j not used
   */
  public ConstructorTest(int i, int j)
  {
    message = "int,int";
  }

  /**
   * Used for testing multiple argument overloading
   * 
   * @param i not used
   * @param s not used
   */
  public ConstructorTest(int i, String s)
  {
    message = "int,String";
  }

  //TODO: adding this makes it fail many tests!
  /*
   * public ConstructorTest(Integer i) { message="int"; }
   */

  /**
   * Test a long constructor
   * 
   * @param l unused
   */
  public ConstructorTest(long l)
  {
    message = "long";
  }

  /**
   * Test an object constructor
   * 
   * @param o unused
   */
  public ConstructorTest(Object o)
  {
    message = "Object";
  }

  /**
   * Test a string constructor
   * 
   * @param s unused
   */
  public ConstructorTest(String s)
  {
    message = "String";
  }

  /**
   * Gets the message produced by the constructor
   * 
   * @return A message that specifies what the arguments were that were given to
   *         the constructor
   */
  public String getMessage()
  {
    return message;
  }
}
