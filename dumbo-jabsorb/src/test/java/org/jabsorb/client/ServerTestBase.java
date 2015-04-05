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

package org.jabsorb.client;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONRPCServlet;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import junit.framework.TestCase;

/**
 * Test case that requires starting the jabsorb server
 */
public class ServerTestBase extends TestCase
{

  /**
   * Encapsulate Jetty hosting server initialization so that
   * we could start it only once during the test run
   */
  static class ServerContext {
	  public Server server;
	  public Context context;
	  public int     port;
	  
	  public ServerContext() throws Exception {
			port= 8083;
			JSONRPCBridge.getGlobalBridge().registerObject("test", new org.jabsorb.test.Test());
			server= new Server(port);
			context= new Context(server, JABSORB_CONTEXT, Context.SESSIONS);
			ServletHolder jsonRpcServlet= new ServletHolder(new JSONRPCServlet());
			// Based on the patch by http://code.google.com/u/cameron.taggart/
			// located at http://code.google.com/p/json-rpc-client/issues/detail?id=1
			jsonRpcServlet.setInitParameter("auto-session-bridge", "0");
			context.addServlet(jsonRpcServlet, "/*");
			server.start();
		}
  }
  
  static ServerContext serverContext;

  static final String JABSORB_CONTEXT = "/jabsorb-trunk";
  @Override
  protected void setUp() throws Exception
  {
	// Prevent multiple startups of the server
    if (serverContext == null)
    {
    	serverContext= new ServerContext();
    }
    super.setUp();
  }
  @Override
  protected void tearDown() throws Exception
  {
    super.tearDown();
  }

  public String getServiceRootURL()
  {
    return "http://localhost:" + Integer.toString(serverContext.port) + JABSORB_CONTEXT;
  }

}
