/*
 * jabsorb - a Java to JavaScript Advanced Object Request Broker
 * http://www.jabsorb.org
 *
 * Copyright 2007-2009 The jabsorb team
 *
 * based on original code from
 * JSON-RPC-Client, a Java client extension to JSON-RPC-Java
 * (C) Copyright CodeBistro 2007, Sasha Ovsankin <sasha at codebistro dot com>
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
package org.jabsorb.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.request.fixups.FixupsCircularReferenceHandler;
import org.jabsorb.serializer.response.fixups.FixupCircRefAndNonPrimitiveDupes;
import org.jabsorb.serializer.response.results.FailedResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A factory to create proxies for access to remote Jabsorb services.
 */
public class Client implements InvocationHandler
{
  /**
   * Maintain a unique id for each message
   */
  private int id = 0;

  /**
   * Gets the id of the next message
   * 
   * @return The id for the next message.
   */
  private synchronized int getId()
  {
    return id++;
  }

  /**
   * Maps proxy keys to proxies
   */
  private final Map<Object, String> proxyMap;

  /**
   * The serializer instance to use.
   */
  private final JSONSerializer serializer;

  /**
   * The transport session to use for this connection
   */
  private final Session session;

  /**
   * Create a client given a session
   * 
   * @param session transport session to use for this connection
   */
  public Client(Session session)
  {
    try
    {
      this.session = session;
      this.proxyMap = new HashMap<Object, String>();
      //TODO: this might need a better way of initialising it
      this.serializer = new JSONSerializer(
          FixupCircRefAndNonPrimitiveDupes.class,
          new FixupsCircularReferenceHandler());
      this.serializer.registerDefaultSerializers();
    }
    catch (Exception e)
    {
      throw new ClientError(e);
    }
  }

  /**
   * Dispose of the proxy that is no longer needed
   * 
   * @param proxy The proxy to close
   */
  public void closeProxy(Object proxy)
  {
    proxyMap.remove(proxy);
  }

  /**
   * Allow access to the serializer
   * 
   * @return The serializer for this class
   */
  public JSONSerializer getSerializer()
  {
    return serializer;
  }

  //This method is public because of the inheritance from the InvokationHandler.
  //It should never be called directly.
  public Object invoke(Object proxyObj, Method method, Object[] args)
      throws Exception
  {
    String methodName = method.getName();
    if (methodName.equals("hashCode"))
    {
      return new Integer(System.identityHashCode(proxyObj));
    }
    else if (methodName.equals("equals"))
    {
      return (proxyObj == args[0] ? Boolean.TRUE : Boolean.FALSE);
    }
    else if (methodName.equals("toString"))
    {
      return proxyObj.getClass().getName() + '@'
          + Integer.toHexString(proxyObj.hashCode());
    }
    return invoke(proxyMap.get(proxyObj), method.getName(), args, method
        .getReturnType());
  }

  /**
   * Create a proxy for communicating with the remote service.
   * 
   * @param key the remote object key
   * @param klass the class of the interface the remote object should adhere to
   * @return created proxy
   */
  public Object openProxy(String key, Class<?> klass)
  {
    Object result = java.lang.reflect.Proxy.newProxyInstance(klass
        .getClassLoader(), new Class[] { klass }, this);
    proxyMap.put(result, key);
    return result;
  }

  /**
   * Generate and throw exception based on the data in the 'responseMessage'
   * 
   * @param responseMessage The error message
   * @throws JSONException Rethrows the exception in the repsonse.
   */
  protected void processException(JSONObject responseMessage)
      throws JSONException
  {
    JSONObject error = (JSONObject) responseMessage.get("error");
    if (error != null)
    {
      Integer code = new Integer(error.has("code") ? error.getInt("code") : 0);
      String trace = error.has("trace") ? error.getString("trace") : null;
      String msg = error.has("msg") ? error.getString("msg") : null;
      throw new ErrorResponse(code, msg, trace);
    }
    throw new ErrorResponse(new Integer(FailedResult.CODE_ERR_PARSE),
        "Unknown response:" + responseMessage.toString(2), null);
  }

  /**
   * Invokes a method for the ciient.
   * 
   * @param objectTag (optional) the name of the object to invoke the method on.
   *          May be null.
   * @param methodName The name of the method to call.
   * @param args The arguments to the method.
   * @param returnType What should be returned
   * @return The result of the call.
   * @throws Exception JSONObject, UnmarshallExceptions or Exceptions from
   *           invoking the method may be thrown.
   */
  private Object invoke(String objectTag, String methodName, Object[] args,
      Class<?> returnType) throws Exception
  {
    JSONObject message;
    String methodTag = objectTag == null ? "" : objectTag + ".";
    methodTag += methodName;

    {
      if (args != null)
      {
        SerializerState state = this.serializer.createSerializerState();
        Object params = serializer.marshall(state, /* parent */
        null, args, JSONSerializer.PARAMETER_FIELD);
        message = state.createObject(JSONSerializer.PARAMETER_FIELD, params);
      }
      else
      {
        message = new JSONObject();
        message.put(JSONSerializer.PARAMETER_FIELD, new JSONArray());
      }
    }
    message.put(JSONSerializer.METHOD_FIELD, methodTag);
    message.put(JSONSerializer.ID_FIELD, getId());

    JSONObject responseMessage = session.sendAndReceive(message);

    if (!responseMessage.has(JSONSerializer.RESULT_FIELD))
      processException(responseMessage);
    Object rawResult = this.serializer.getRequestParser().unmarshall(
        responseMessage, JSONSerializer.RESULT_FIELD);
    if (returnType.equals(Void.TYPE))
    {
      return null;
    }
    else if (rawResult == null)
    {
      processException(responseMessage);
    }
    {
      SerializerState state = this.serializer.createSerializerState();
      Object toReturn = serializer.unmarshall(state, returnType, rawResult);

      return toReturn;
    }
  }
}
