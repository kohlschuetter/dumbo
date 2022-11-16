package com.kohlschutter.dumbo;

import java.io.IOException;
import java.util.regex.Pattern;

import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Map .js URLs to .jsp.js (or .js.jsp) where required.
 * 
 * This allows to internally use JSP for JavaScript content without exposing it.
 * 
 * <p>
 * Example usage (in a {@code .jsp.js} file):
 * </p>
 * <p>
 * <code> 
 * const contextPath = '<%@page session="false" contentType="application/javascript" %><%=
 * application.getContextPath() %>';
 * </code>
 * </p>
 * 
 * @author Christian Kohlschütter
 */
public class JspJsServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private DefaultServlet defaultServlet;
  private JettyJspServlet jspServlet;
  private static final Pattern PAT_JS = Pattern.compile("\\.js$");

  @Override
  public void init() throws ServletException {
    defaultServlet = (DefaultServlet) ((ServletHolder) getServletContext().getAttribute("holder."
        + DefaultServlet.class.getName())).getServlet();
    jspServlet = (JettyJspServlet) ((ServletHolder) getServletContext().getAttribute("holder."
        + JettyJspServlet.class.getName())).getServlet();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    String requestURI = req.getRequestURI();
    String pathInContext = requestURI.substring(req.getContextPath().length());

    if (defaultServlet.getResource(pathInContext).exists()) {
      if (pathInContext.contains(".jsp.js")) {
        jspServlet.service(req, resp);
      } else {
        defaultServlet.service(req, resp);
      }
      return;
    } else if (defaultServlet.getResource(pathInContext + ".jsp").exists()) {
      req.getRequestDispatcher(pathInContext + ".jsp").forward(req, resp);
    } else {
      String path = PAT_JS.matcher(pathInContext).replaceFirst(".jsp.js");
      if (defaultServlet.getResource(path).exists()) {
        req.getRequestDispatcher(path).forward(req, resp);
      } else {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    }
  }
}
