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
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public interface ITest
{
  static public class Waggle implements Serializable
  {

    private final static long serialVersionUID = 2;

    private int               baz;

    private String            bang;

    private Integer           bork;

    public Waggle()
    {
      //This empty default constructor is used
    }

    public Waggle(int i)
    {
      baz = i;
      bang = "!";
    }

    public int getBaz()
    {
      return baz;
    }

    public void setBaz(int baz)
    {
      this.baz = baz;
    }

    public String getBang()
    {
      return bang;
    }

    public void setBang(String bang)
    {
      this.bang = bang;
    }

    public Integer getBork()
    {
      return bork;
    }

    public void setBork(Integer bork)
    {
      this.bork = bork;
    }

    @Override
    public String toString()
    {
      return "waggle " + baz + " and " + bang;
    }
  }

  static public class Wiggle implements Serializable
  {

    private final static long serialVersionUID = 2;

    private String            foo;

    private int               bar;

    public Wiggle()
    {
      //This empty default constructor is used
    }

    public Wiggle(int i)
    {
      bar = i;
      foo = "foo";
    }

    public String getFoo()
    {
      return foo;
    }

    public void setFoo(String foo)
    {
      this.foo = foo;
    }

    public int getBar()
    {
      return bar;
    }

    public void setBar(int bar)
    {
      this.bar = bar;
    }

    @Override
    public String toString()
    {
      return "wiggle " + foo + " and " + bar;
    }
  }

  void voidFunction();

  String[] echo(String strings[]);

  int echo(int i);

  int[] echo(int i[]);

  String echo(String message);

  /**
   * Made to test issue 47
   * @param object the array to echo
   * @return the same array given.
   */
  Object[] echoArray(Object[] object);
  
  List<?> echoList(List<?> l);

  byte[] echoByteArray(byte ba[]);

  char[] echoCharArray(char ca[]);

  char echoChar(char c);

  boolean echoBoolean(boolean b);

  boolean[] echoBooleanArray(boolean ba[]);

  Integer[] echoIntegerArray(Integer i[]);

  Integer echoIntegerObject(Integer i);

  String echoOverloadedObject(Number i);
  
  String echoOverloadedObject(Boolean s);
  
  Long echoLongObject(Long l);

  Float echoFloatObject(Float f);

  Double echoDoubleObject(Double d);

  Date echoDateObject(Date d);
  
  java.sql.Date echoSQLDateObject(java.sql.Date d);

  Object echoObject(Object o);

  Object echoObjectArray(Object[] o);

  int[] anArray();

  ArrayList<?> anArrayList();

  Vector<?> aVector();

  List<?> aList();

  Set<?> aSet();

  BeanA aBean();
  
  Map<?,?> complexKeyedMap();

  Enum<?> anEnum();
  
  Hashtable<?,?> aHashtable();

  /**
   * Checks bug #18 is fixed
   * @return A map with a null key.
   */
  Map<?,?> nullKeyedMap();
  
  String[] twice(String string);

  String concat(String msg1, String msg2);

  ITest.Wiggle echo(ITest.Wiggle wiggle);

  ITest.Waggle echo(ITest.Waggle waggle);

  ArrayList<?> aWiggleArrayList(int numWiggles);

  ArrayList<?> aWaggleArrayList(int numWaggles);

  String wigOrWag(ArrayList<?> al);
}
