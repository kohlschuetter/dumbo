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
/**
 * 
 */
package org.jabsorb.util;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Converts messages from browser-local hiveshare to inter-hiveshare formats
 * 
 * @author William Becker
 */
public class MessageConverter {
  /**
   * Bridge used to communicate between the browser and the local hiveshare server
   */
  private final JSONRPCBridge firstBridge;

  /**
   * Bridge used to communicate between hiveshare systems.
   */
  private final JSONRPCBridge secondBridge;

  /**
   * Creates a new MessageConverter
   * 
   * @param firstBridge A bridge to convert data to/from
   * @param secondBridge Another bridge to convert data to/from
   */
  public MessageConverter(final JSONRPCBridge firstBridge, final JSONRPCBridge secondBridge) {
    this.firstBridge = firstBridge;
    this.secondBridge = secondBridge;
  }

  /**
   * Converts objects from the first bridge's format to the second bridge's format.
   * 
   * @param message The message to convert
   * @param dataKey The key under which the data is stored
   * @throws JSONException when the json cannot be parsed
   * @throws UnmarshallException When the message cannot be read
   * @throws MarshallException When the message cannot be created
   * @return The converted message
   */
  public JSONObject convertObjectFromFirstToSecond(final JSONObject message, final String dataKey)
      throws JSONException, UnmarshallException, MarshallException {
    return this.doConvert(message, dataKey, this.firstBridge, this.secondBridge);
  }

  /**
   * Converts objects from the second bridge's format to the first bridge's format.
   * 
   * @param message The message to convert
   * @param dataKey The key under which the data is stored
   * @return The converted message
   * @throws JSONException when the json cannot be parsed
   * @throws UnmarshallException when the message cannot be read
   * @throws MarshallException When the message cannot be created
   */
  public JSONObject convertObjectFromSecondToFirst(final JSONObject message, final String dataKey)
      throws JSONException, UnmarshallException, MarshallException {
    return this.doConvert(message, dataKey, this.secondBridge, this.firstBridge);
  }

  /**
   * Actually does the conversion
   * 
   * @param message The message to convert
   * @param dataKey Whether the data is stored
   * @param from The bridge to convert from
   * @param to The bridge to convert to
   * @return The converted message
   * @throws JSONException when the json cannot be parsed
   * @throws UnmarshallException when the message cannot be read
   * @throws MarshallException When the message cannot be created
   */
  private JSONObject doConvert(final JSONObject message, final String dataKey,
      final JSONRPCBridge from, final JSONRPCBridge to) throws JSONException, UnmarshallException,
      MarshallException {
    final Object o;

    // Convert the data to an unmarshalled state
    {
      if (message != null) {
        final Object toUnmarshall = from.getSerializer().getRequestParser().unmarshall(message,
            dataKey);
        o = from.getSerializer().unmarshall(null, toUnmarshall);
      } else {
        o = null;
      }
    }
    // Remarshall the data
    {
      final SerializerState state = to.getSerializer().createSerializerState();
      final Object marshalled = to.getSerializer().marshall(state, null, o, "r");
      return state.createObject(dataKey, marshalled);
    }
  }
}