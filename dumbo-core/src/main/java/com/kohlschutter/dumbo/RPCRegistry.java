/*
 * Copyright 2022-2025 Christian Kohlschütter
 * Copyright 2014,2015 Evernote Corporation.
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
package com.kohlschutter.dumbo;

/**
 * Allows registering service interfaces and their implementations to be used by some kind of RPC
 * infrastructure.
 */
public interface RPCRegistry {
  /**
   * Registers a service with the RPC subsystem.
   *
   * @param serviceInterface The service interface
   * @param instance The service implementation.
   */
  <T> void registerRPCService(Class<T> serviceInterface, T instance);

  <T> T getRPCService(Class<T> serviceInterface);
}
