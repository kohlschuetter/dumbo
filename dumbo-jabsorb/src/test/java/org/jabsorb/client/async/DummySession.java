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
package org.jabsorb.client.async;

import org.jabsorb.client.Session;
import org.jabsorb.serializer.response.results.SuccessfulResult;
import org.json.JSONException;
import org.json.JSONObject;

class DummySession implements Session {
  private final int duration;
  private final String response;

  public DummySession(final int duration, final String response) {
    super();
    this.duration = duration;
    this.response = response;
  }

  public void close() {
    // nothing
  }

  public JSONObject sendAndReceive(final JSONObject message) {
    try {
      Thread.sleep(duration);
      return new SuccessfulResult("1", DummySession.this.response).createOutput();
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    } catch (final JSONException e) {
      throw new RuntimeException(e);
    }
  }
}