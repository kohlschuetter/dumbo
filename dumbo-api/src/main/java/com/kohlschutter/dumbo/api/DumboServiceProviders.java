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
package com.kohlschutter.dumbo.api;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * Provides access to all implementations registered for a Dumbo service.
 *
 * @author Christian Kohlschütter
 */
@FunctionalInterface
public interface DumboServiceProviders {
  /**
   * Provides a stream of implementations for the Dumbo service identified by the given service
   * interface.
   *
   * @param <T> The type of the service.
   * @param clazz The service interface.
   * @return The stream of service implementations.
   */
  <T> Collection<T> getDumboServices(Class<T> clazz);

  /**
   * Returns a stream of any registered implementation for the given service, potentially an empty
   * stream.
   * <p>
   * The implementations are resolved via <em>Service Provider Interface</em>.
   *
   * @param <T> The service type.
   * @param service The service interface.
   * @return The stream of registered implementations, potentially empty.
   */
  @SuppressWarnings("unchecked")
  static <T> Stream<T> allRegisteredImplementationsForService(Class<T> service) {
    return ((Collection<T>) InternalCache.DUMBO_SERVICE_PROVIDERS.computeIfAbsent(service, (s) -> {
      Stream<T> stream = ServiceLoader.load(DumboServiceProviders.class).stream().map((p) -> p
          .get()).flatMap((p) -> p.getDumboServices(service).stream());

      return stream.filter((p) -> p != null).toList();
    })).stream();
  }
}
