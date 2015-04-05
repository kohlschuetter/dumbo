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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Use this result for sending messages to client containing errors.
 * 
 * @author William Becker
 */
public class FailedResult extends JSONRPCResult
{
  /**
   * Gets the error that caused the failure.
   * 
   * @return The error object
   */
  public Object getError()
  {
    return error;
  }

  /**
   * Denotes that an error occured while parsing the request.
   */
  public final static int CODE_ERR_PARSE = 590;

  /**
   * Denotes (when calling a constructor) that no method was found with the
   * given name/arguments.
   */
  public final static int CODE_ERR_NOCONSTRUCTOR = 594;

  /**
   * Denotes (when using a callable reference) that no method was found with the
   * given name and number of arguments.
   */
  public final static int CODE_ERR_NOMETHOD = 591;

  /**
   * Denotes that an error occured while unmarshalling the request.
   */
  public final static int CODE_ERR_UNMARSHALL = 592;

  /**
   * Denotes that an error occured while marshalling the response.
   */
  public final static int CODE_ERR_MARSHALL = 593;

  /**
   * Denotes that an error occured while applying the fixup data for circular
   * references/duplicates.
   */
  public final static int CODE_ERR_FIXUP = 594;

  /**
   * The error method shown when an error occured while parsing the request.
   */
  public final static String MSG_ERR_PARSE = "couldn't parse request arguments";

  /**
   * The error method shown when no constructor was found with the given name.
   */
  public static final String MSG_ERR_NOCONSTRUCTOR = "constructor not found";

  /**
   * The error method shown when no method was found with the given name and
   * number of arguments.
   */
  public final static String MSG_ERR_NOMETHOD = "method with the requested number of arguments not found (session may"
      + " have timed out)";

  /**
   * The error method shown when something in the fixups was amiss.
   */
  public final static String MSG_ERR_FIXUP = "invalid or unexpected data in fixups";

  /**
   * An error code if a problem occured (CODE_SUCCESS otherwise)
   */
  private final int errorCode;

  /**
   * The error that caused the failure.
   */
  private final Object error;

  /**
   * Creates a new FailedResult.
   * 
   * @param errorCode The error code. This should be one of the error codes
   *          defined in this class.
   * @param id The id of the response.
   * @param error The error that caused the failure.
   */
  public FailedResult(int errorCode, Object id, Object error)
  {
    super(id);
    this.errorCode = errorCode;
    this.error = error;
  }

  @Override
  protected JSONObject createOutput() throws JSONException
  {
    JSONObject o = super._createOutput();
    JSONObject err = new JSONObject();
    err.put("code", new Integer(errorCode));
    err.put("msg", error);
    o.put("error", err);
    return o;
  }
}
