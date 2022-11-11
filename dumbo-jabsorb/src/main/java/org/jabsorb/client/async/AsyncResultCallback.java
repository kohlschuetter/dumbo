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

import java.util.concurrent.Future;

/**
 * This interface specifies a method that is called when the asynchronous operation is completed.
 *
 * @author matthijs
 *
 */
public interface AsyncResultCallback<S, R, C> {
  /**
   * Method to be called when the asynchronous operation is completed
   * 
   * @param source The source of the callback, usually the object that contains the asynchronous
   *          operation
   * @param result The result of the operation
   * @param context Optional context data
   */
  public void onAsyncResult(S source, Future<R> result, C context);
}
