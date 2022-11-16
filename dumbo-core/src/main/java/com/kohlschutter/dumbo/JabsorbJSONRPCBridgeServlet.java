/*
 * Copyright 2022 Christian Kohlschütter
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jetty.io.EofException;
import org.jabsorb.ExceptionTransformer;
import org.jabsorb.JSONRPCBridge;
import org.jabsorb.serializer.response.results.FailedResult;
import org.jabsorb.serializer.response.results.JSONRPCResult;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.kohlschutter.dumbo.console.ConsoleService;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * A Jabsorb-based JSON-RPC Servlet.
 */
public class JabsorbJSONRPCBridgeServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Charset UTF_8 = Charset.forName("UTF-8");
  private JSONRPCBridge bridge;
  private JSONRPCRegistryImpl registry;
  private final ThreadLocal<String> tlMethod = new ThreadLocal<>();
  private ServerApp app;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    try {

      ServletContext ctx = config.getServletContext();
      this.app = (ServerApp) ctx.getAttribute(ServerApp.class.getName());

      bridge = new JSONRPCBridge();
      bridge.setExceptionTransformer(new ExceptionTransformer() {
        private static final long serialVersionUID = 1L;

        @Override
        public Object transform(Throwable t) {
          System.err.println("Error during JSON method call: " + tlMethod.get());
          t.printStackTrace();
          if (t instanceof PermanentRPCException) {
            throw (PermanentRPCException) t;
          }
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
    } catch (RuntimeException | Error e) {
      e.printStackTrace();
      throw e;
    }
  }

  private static final class JSONRPCRegistryImpl implements RPCRegistry {
    private final JSONRPCBridge bridge;
    private ConsoleService consoleService;

    public JSONRPCRegistryImpl(final JSONRPCBridge bridge) {
      this.bridge = bridge;
    }

    @Override
    public <T> void registerRPCService(Class<T> interfaze, T instance) {
      Objects.requireNonNull(interfaze, "Interface class must not be null");
      Objects.requireNonNull(instance, "Instance must not be null");
      if (ConsoleService.class == interfaze && consoleService == null) {
        this.consoleService = (ConsoleService) instance;
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
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      doPost0(request, response);
    } catch (EofException | ClosedByInterruptException e) {
      // connection terminated; ignore
      return;
    } catch (ServletException | IOException | RuntimeException | Error e) {
      e.printStackTrace();
      throw e;
    }
  }

  private void doPost0(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    request.setCharacterEncoding("UTF-8");
    response.setCharacterEncoding("UTF-8");

    HttpSession context = request.getSession(true);
    if (context == null) {
      // unlikely
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "No session");
      return;
    }

    boolean triggerOnAppLoaded = false;

    int maxPagesPerSession = app.getMaximumPagesPerSession();

    DumboSession dumboSession;
    String newServerURL = null;
    if (maxPagesPerSession != 0) {
      String pageId = request.getParameter("pageId");
      if (pageId == null && maxPagesPerSession != 0) {
        triggerOnAppLoaded = true;
        pageId = DumboSession.newPageId(context, maxPagesPerSession);
        newServerURL = request.getRequestURI();
        String qs = request.getQueryString();
        if (qs != null && !qs.isEmpty()) {
          newServerURL += "?" + qs + "&pageId=" + response.encodeURL(pageId);
        } else {
          newServerURL += "?pageId=" + response.encodeURL(pageId);
        }
      }

      dumboSession = DumboSession.getDumboSession(context, pageId);
      if (dumboSession == null) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid pageId");
        return;
      }
    } else {
      dumboSession = null;
    }

    DumboSession.setSessionTL(dumboSession);
    JSONRPCResult result;
    try {
      JSONTokener jt = new JSONTokener(request.getReader());
      JSONObject jsonRequest = new JSONObject(jt);

      String method = jsonRequest.getString("method");
      tlMethod.set(method);
      if (registry.consoleService != null) {
        if (dumboSession != null && !method.startsWith("ConsoleService.") && !method.startsWith(
            "system.")) {
          ConsoleService service = ((ConsoleImpl) dumboSession.getConsole()).getConsoleService();
          synchronized (service) {
            service.notifyAll();
          }
        }
      }

      result = bridge.call(new Object[] {request, response}, jsonRequest);
    } catch (PermanentRPCException e) {
      String message = e.getMessage();

      int statusCode = HttpServletResponse.SC_OK;

      if (e instanceof NoSessionException) {
        if (app.getMaximumPagesPerSession() == 0) {
          statusCode = HttpServletResponse.SC_FORBIDDEN;
        } else {
          statusCode = HttpServletResponse.SC_UNAUTHORIZED;
        }
      }

      if (statusCode != HttpServletResponse.SC_OK) {
        response.sendError(statusCode);
        response.flushBuffer();
        return;
      } else {
        if (message == null) {
          message = "Not allowed";
        }
        result = new FailedResult(HttpServletResponse.SC_FORBIDDEN, null, message);
      }
    } catch (UnsupportedEncodingException | RuntimeException e) {
      result = new FailedResult(FailedResult.CODE_ERR_PARSE, null, FailedResult.MSG_ERR_PARSE);
      triggerOnAppLoaded = false;
    } finally {
      tlMethod.set(null);
      DumboSession.removeSessionTL();
    }

    ByteBuffer byteBuffer = UTF_8.encode(result.toJSONString(newServerURL));
    response.setContentLength(byteBuffer.remaining());
    response.setContentType("application/json;charset=utf-8");
    try (OutputStream out = response.getOutputStream();
        WritableByteChannel channel = Channels.newChannel(out)) {
      channel.write(byteBuffer);
      out.flush();
      response.flushBuffer();
    }

    if (triggerOnAppLoaded) {
      CompletableFuture.runAsync(() -> {
        app.onAppLoaded(dumboSession);
      });
    }
  }
}
