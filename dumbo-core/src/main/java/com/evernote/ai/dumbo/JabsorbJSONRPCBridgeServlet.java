/**
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
package com.evernote.ai.dumbo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Objects;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jabsorb.ExceptionTransformer;
import org.jabsorb.JSONRPCBridge;
import org.jabsorb.serializer.response.results.FailedResult;
import org.jabsorb.serializer.response.results.JSONRPCResult;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.evernote.ai.dumbo.console.ConsoleService;

/**
 * A Jabsorb-based JSON-RPC Servlet.
 */
public class JabsorbJSONRPCBridgeServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Charset UTF_8 = Charset.forName("UTF-8");
  private JSONRPCBridge bridge;
  private JSONRPCRegistryImpl registry;
  private final ThreadLocal<String> tlMethod = new ThreadLocal<>();

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    ServletContext ctx = config.getServletContext();
    ServerApp app = (ServerApp) ctx.getAttribute("app");

    bridge = new JSONRPCBridge();
    bridge.setExceptionTransformer(new ExceptionTransformer() {
      private static final long serialVersionUID = 1L;

      @Override
      public Object transform(Throwable t) {
        System.err.println("Error during JSON method call: " + tlMethod.get());
        t.printStackTrace();
        return t;
      }
    });

    registry = new JSONRPCRegistryImpl(bridge);
    app.initRPCInternal(registry);
    app.initRPC(registry);

    if (app.isStaticDesignMode()) {
      // don't run actual app in design mode
      return;
    }

    app.onStart();
  }

  private static final class JSONRPCRegistryImpl implements RPCRegistry {
    private final JSONRPCBridge bridge;
    private ConsoleService console;

    public JSONRPCRegistryImpl(final JSONRPCBridge bridge) {
      this.bridge = bridge;
    }

    @Override
    public <T> void registerRPCService(Class<T> interfaze, T instance) {
      Objects.requireNonNull(interfaze, "Interface class must not be null");
      Objects.requireNonNull(instance, "Instance must not be null");
      if (ConsoleService.class == interfaze && console == null) {
        this.console = (ConsoleService) instance;
      }
      bridge.registerObject(interfaze.getSimpleName(), instance, interfaze);
    }
  }

  @Override
  public void destroy() {
    bridge = null;
    registry = null;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    request.setCharacterEncoding("UTF-8");
    response.setCharacterEncoding("UTF-8");

    JSONRPCResult result;
    try {
      JSONTokener jt = new JSONTokener(request.getReader());
      JSONObject jsonRequest = new JSONObject(jt);

      String method = jsonRequest.getString("method");
      tlMethod.set(method);
      if (registry.console != null) {
        if (!method.startsWith("ConsoleService.") && !method.startsWith("system.")) {
          synchronized (registry.console) {
            registry.console.notifyAll();
          }
        }
      }

      result = bridge.call(new Object[] {request, response}, jsonRequest);
    } catch (UnsupportedEncodingException | RuntimeException e) {
      result = new FailedResult(FailedResult.CODE_ERR_PARSE, null, FailedResult.MSG_ERR_PARSE);
    } finally {
      tlMethod.set(null);
    }

    ByteBuffer byteBuffer = UTF_8.encode(result.toString());
    response.setContentLength(byteBuffer.remaining());
    response.setContentType("application/json;charset=utf-8");
    try (OutputStream out = response.getOutputStream();
        WritableByteChannel channel = Channels.newChannel(out)) {
      channel.write(byteBuffer);
      out.flush();
    } catch (ClosedByInterruptException e) {
      // ignore
    }
  }
}
