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
package org.jabsorb.client.async;

import java.util.HashMap;
import java.util.Map;

import org.jabsorb.JSONSerializer;
import org.jabsorb.client.ClientError;
import org.jabsorb.serializer.request.fixups.FixupsCircularReferenceHandler;
import org.jabsorb.serializer.response.fixups.FixupCircRefAndNonPrimitiveDupes;

/**
 * A factory to create proxies for access to remote Jabsorb services.
 */
public class AsyncClient {

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
	private final AsyncSession session;

	/**
	 * Create a client given a session
	 *
	 * @param session
	 *            transport session to use for this connection
	 */
	public AsyncClient(final AsyncSession session) {
		try {
			this.session = session;
			this.proxyMap = new HashMap<Object, String>();
			// TODO: this might need a better way of initialising it
			this.serializer = new JSONSerializer(
					FixupCircRefAndNonPrimitiveDupes.class,
					new FixupsCircularReferenceHandler());
			this.serializer.registerDefaultSerializers();
		} catch (final Exception e) {
			throw new ClientError(e);
		}
	}

	/**
	 * Create a proxy for communicating with the remote service.
	 *
	 * @param key
	 *            the remote object key
	 * @param klass
	 *            the class of the interface the remote object should adhere to
	 * @return created proxy
	 */
	public Object openProxy(final String key, final Class<?> klass) {
		final Object result = java.lang.reflect.Proxy.newProxyInstance(
				klass.getClassLoader(), new Class[] { klass, AsyncProxy.class }, new AsyncProxyHandler(key, session, serializer));

		proxyMap.put(result, key);
		return result;
	}

	/**
	 * Dispose of the proxy that is no longer needed
	 *
	 * @param proxy
	 *            The proxy to close
	 */
	public void closeProxy(final Object proxy) {
		proxyMap.remove(proxy);
	}

	/**
	 * Allow access to the serializer
	 *
	 * @return The serializer for this class
	 */
	public JSONSerializer getSerializer() {
		return serializer;
	}
}
