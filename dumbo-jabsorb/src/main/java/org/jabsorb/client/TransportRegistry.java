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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * A registry of transports serving JSON-RPC-Client
 */
public class TransportRegistry
{

  /**
   * A factory used to create transport sessions. Register with
   * #registerTransport.
   */
  public interface SessionFactory
  {
    /**
     * Creates the new session
     * 
     * @param uri URI used to open this session
     * @return The new session
     */
    Session newSession(URI uri);
  }

  /**
   * Maintains singleton instance of this class
   */
  private static TransportRegistry singleton;

  /**
   * Use this function when there is no IOC container to rely on creating the
   * factory.
   * 
   * @return singleton instance of the class, created if necessary.
   */
  public synchronized static TransportRegistry i()
  {
    if (singleton == null)
    {
      singleton = new TransportRegistry();
    }
    return singleton;
  }

  /**
   * Maps schemes (eg "http") to session factories
   */
  private final Map<String, SessionFactory> registry;

  /**
   * Creates a new TransportRegistry
   */
  public TransportRegistry()
  {
    this.registry = new HashMap<String, SessionFactory>();
  }

  /**
   * Create a session from 'uriString' using one of registered transports.
   * 
   * @param uriString The uri of the session
   * @return a URLConnectionSession
   */
  public Session createSession(String uriString)
  {
    try
    {
      URI uri = new URI(uriString);
      SessionFactory found = registry.get(uri.getScheme());
      if (found != null)
      {
        return found.newSession(uri);
      }
      // Fallback
      return new URLConnectionSession(uri.toURL());
    }
    catch (Exception e)
    {
      throw new ClientError(e);
    }
  }

  /**
   * Register a factory for a transport type
   * 
   * @param scheme The transport type
   * @param factory The session factory for the scheme
   */
  public void registerTransport(String scheme, SessionFactory factory)
  {
    registry.put(scheme, factory);
  }

}
