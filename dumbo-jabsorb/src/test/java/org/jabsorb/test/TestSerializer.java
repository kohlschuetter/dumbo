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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.request.fixups.FixupsCircularReferenceHandler;
import org.jabsorb.serializer.response.fixups.FixupCircRefAndNonPrimitiveDupes;
import org.json.JSONObject;

public class TestSerializer extends TestCase
{
  static final Map<Integer, String> TEST_MAP1 = new HashMap<Integer, String>();
  static
  {
    TEST_MAP1.put(new Integer(1), "1");
    TEST_MAP1.put(new Integer(2), "2");
  }

  JSONSerializer ser;

  SerializerState marshallerState;

  @Override
  protected void setUp() throws Exception
  {
    ser = new JSONSerializer(FixupCircRefAndNonPrimitiveDupes.class, new FixupsCircularReferenceHandler());
    ser.registerDefaultSerializers();
    ser.setMarshallClassHints(true);
  }

  public void dontTestExtendedMaps() throws Exception
  {
    JSONObject json = (JSONObject) ser.marshall(marshallerState, null,
        TEST_MAP1, "testMap1");
    System.out.println("Serialized: ");
    System.out.println(json.toString(2));
    HashMap<Integer, String> unmarshalled = (HashMap<Integer, String>) ser
        .unmarshall(HashMap.class, json);
    assertEquals(TEST_MAP1, unmarshalled);
  }

  static final HashMap<Integer, String> TEST_MAP2 = new HashMap<Integer, String>();
  static
  {
    TEST_MAP2.put(new Integer(1), "1");
    TEST_MAP2.put(new Integer(2), "2");
  }

  public void dontTestMaps() throws Exception
  {
    JSONObject json = (JSONObject) ser.marshall(marshallerState, null,
        TEST_MAP2, "testMap2");
    System.out.println("Serialized: ");
    System.out.println(json.toString(2));
    HashMap<Integer, String> unmarshalled = (HashMap<Integer, String>) ser
        .unmarshall(HashMap.class, json);
    assertEquals(TEST_MAP2, unmarshalled);
  }

  public void testWaggle() throws Exception
  {
    ITest.Waggle waggle = new ITest.Waggle(1);
    JSONObject json1 = (JSONObject) ser.marshall(null,
        waggle, "waggle");
    ITest.Waggle unmarshalled = (ITest.Waggle) ser.unmarshall(
        ITest.Waggle.class, json1);
    assertEquals(waggle.toString(), unmarshalled.toString());
    JSONObject json2 = (JSONObject) ser.marshall(null,
        unmarshalled, "waggle");
    assertEquals(json1.toString(), json2.toString());
  }
}
