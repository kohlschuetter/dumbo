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

package org.jabsorb.serializer.response.results;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Special result for when a specific exception is thrown by the server.
 */
public class RemoteException extends FailedResult
{
  /**
   * Denotes that an exception was thrown on the server
   */
  public final static int CODE_REMOTE_EXCEPTION = 490;

  /**
   * Creates a new RemoteException
   * 
   * @param id The id of the response
   * @param error The Exception which was thrown.
   */
  public RemoteException(Object id, Object error)
  {
    super(CODE_REMOTE_EXCEPTION, id, error);
  }

  @Override
  protected JSONObject createOutput() throws JSONException
  {
    JSONObject o = super.createOutput();
    if (getError() instanceof Throwable)
    {
      Throwable e = (Throwable) getError();
      CharArrayWriter caw = new CharArrayWriter();
      e.printStackTrace(new PrintWriter(caw));
      JSONObject err = new JSONObject();
      err.put("code", new Integer(CODE_REMOTE_EXCEPTION));
      err.put("msg", e.getMessage());
      err.put("trace", caw.toString());
      o.put("error", err);
    }
    else
    {
      // When using a customized implementation of ExceptionTransformer
      // an error result may be something other than Throwable. In this
      // case, it has to be a JSON compatible object, we will just store it
      // to the 'error' property of the response. 
      o.put("error", getError());
    }

    return o;
  }
}
