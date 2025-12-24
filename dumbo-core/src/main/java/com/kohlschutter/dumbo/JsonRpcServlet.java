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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.AccessibleObject;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jetty.io.EofException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;
import com.kohlschutter.dumbo.annotations.DumboService;
import com.kohlschutter.dumbo.api.DumboSession;
import com.kohlschutter.dumbo.console.ConsoleService;
import com.kohlschutter.dumbo.exceptions.NoSessionException;
import com.kohlschutter.dumbo.exceptions.PermanentRPCException;
import com.kohlschutter.dumborb.ExceptionTransformer;
import com.kohlschutter.dumborb.JSONRPCBridge;
import com.kohlschutter.dumborb.callback.InvocationCallback;
import com.kohlschutter.dumborb.security.ClassResolver;
import com.kohlschutter.dumborb.serializer.response.results.FailedResult;
import com.kohlschutter.dumborb.serializer.response.results.JSONRPCResult;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * A Dumborb-based JSON-RPC Servlet.
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
class JsonRpcServlet extends HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger(JsonRpcServlet.class);
  private static final long serialVersionUID = 1L;
  private transient JSONRPCBridge bridge;
  private transient JSONRPCRegistryImpl registry;
  private final transient ThreadLocal<JsonRpcContext> tlContext = new ThreadLocal<>();
  private transient ServerApp app;
  private transient DumboServerImpl server;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    try {

      ServletContext ctx = config.getServletContext();
      this.app = (ServerApp) ctx.getAttribute(ServerApp.class.getName());

      bridge = new JSONRPCBridge(ClassResolver.withDefaults()); // FIXME
      bridge.setExceptionTransformer(new ExceptionTransformer() {
        @Override
        @SuppressWarnings("PMD.GuardLogStatement")
        public Object transform(Throwable t) {
          JsonRpcContext context = tlContext.get();

          if (LOG.isWarnEnabled()) {
            if (LOG.isInfoEnabled() && context.isErrorStackTraces()) {
              LOG.warn("Error in JSON method {}: {}", context.getMethod(), t);
            } else {
              LOG.warn("Error in JSON method {}: {}", context.getMethod(), t.toString());
            }
          }
          if (t instanceof PermanentRPCException) {
            throw (PermanentRPCException) t;
          }
          return t;
        }
      });
      bridge.registerCallback(new InvocationCallback<@NonNull HttpServletRequest>() {

        @Override
        public void preInvoke(@NonNull HttpServletRequest context, Object instance,
            AccessibleObject accessibleObject, Object[] arguments) throws Exception {
          ThreadLocalRequestAccess.setHttpServletRequest(context);
        }

        @Override
        public void postInvoke(@NonNull HttpServletRequest context, Object instance,
            AccessibleObject accessibleObject, Object result, Throwable error) throws Exception {
          ThreadLocalRequestAccess.setHttpServletRequest(null);
        }
      }, HttpServletRequest.class);

      registry = new JSONRPCRegistryImpl(bridge);

      app.initRPC(registry);

      if (app.isStaticDesignMode()) {
        // don't run actual app in design mode
        return;
      }

      app.onStart();
    } catch (RuntimeException | Error e) {
      LOG.error("Failure on init", e);
      throw e;
    }
  }

  public <T> T getRPCService(Class<T> serviceInterface) {
    return registry.getRPCService(serviceInterface);
  }

  private static final class JSONRPCRegistryImpl implements RPCRegistry {
    private final JSONRPCBridge bridge;
    private final Map<Class<?>, Object> classToInstance = new HashMap<>();
    private ConsoleService consoleService;

    public JSONRPCRegistryImpl(final JSONRPCBridge bridge) {
      this.bridge = bridge;
    }

    @Override
    public <T extends Object> void registerRPCService(Class<T> serviceInterface, T instance) {
      Objects.requireNonNull(serviceInterface, "Interface class must not be null");
      Objects.requireNonNull(instance, "Instance must not be null");

      if (classToInstance.containsKey(serviceInterface)) {
        throw new IllegalStateException("Already registered");
      }

      // FIXME
      if (ConsoleService.class == serviceInterface) {
        this.consoleService = (ConsoleService) instance;
      }

      classToInstance.put(serviceInterface, instance);

      String rpcName = serviceInterface.getName();
      if (serviceInterface.isAnnotationPresent(DumboService.class)) {
        DumboService service = serviceInterface.getAnnotation(DumboService.class);
        String declaredRpcName = service.rpcName();
        if (!declaredRpcName.isEmpty()) {
          rpcName = declaredRpcName;
        }
      }

      bridge.registerObject(rpcName, instance, serviceInterface);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getRPCService(Class<T> serviceInterface) {
      return (T) classToInstance.get(serviceInterface);
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
      LOG.info("Exception in service", e);
      throw e;
    }
  }

  @SuppressWarnings({
      "PMD.NcssCount", "PMD.CognitiveComplexity", "PMD.NPathComplexity",
      "PMD.CyclomaticComplexity"})
  @SuppressFBWarnings("URL_REWRITING")
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
    String pageId = request.getParameter("pageId");
    if (maxPagesPerSession != 0) {
      if (pageId == null && maxPagesPerSession != 0) {
        triggerOnAppLoaded = true;
        pageId = DumboSessionImpl.newPageId(context, maxPagesPerSession);
        newServerURL = request.getRequestURI();
        String qs = request.getQueryString();
        newServerURL += "?pageId=" + response.encodeURL(pageId);
        if (qs != null && !qs.isEmpty()) {
          newServerURL += "&" + qs;
        }
      }

      dumboSession = DumboSessionImpl.getDumboSession(context, pageId);
      if (dumboSession == null) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid pageId");
        return;
      }
    } else {
      dumboSession = null;
    }

    DumboSessionImpl.setSession(dumboSession);
    JSONRPCResult result;
    try {
      JSONTokener jt = new JSONTokener(request.getReader());
      JSONObject jsonRequest;
      try {
        jsonRequest = new JSONObject(jt);
      } catch (JSONException e) {
        if (LOG.isInfoEnabled()) {
          LOG.info("Could not parse JSON request; pageId=" + pageId, e);
        }
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        response.flushBuffer();
        return;
      }

      String method = jsonRequest.getString("method");

      JsonRpcContext rpcContext = new JsonRpcContext(method);
      if (server != null) {
        String dumboSecret = request.getHeader(DumboURLConnectionSession.KEY);
        server.getJsonRpcTestSecretConsumer(dumboSecret).accept(rpcContext);
      }

      tlContext.set(rpcContext);

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

      if (e instanceof NoSessionException) { // NOPMD
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
      tlContext.set(null);
      DumboSessionImpl.removeSession();
    }

    ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(result.toJSONString(newServerURL));
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

  void setServer(DumboServerImpl server) {
    this.server = server;
  }

  JSONRPCBridge getBridge() {
    return bridge;
  }
}
