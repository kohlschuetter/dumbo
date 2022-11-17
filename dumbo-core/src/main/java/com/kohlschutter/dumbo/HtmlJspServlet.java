package com.kohlschutter.dumbo;

import java.io.IOException;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Map .html URLs to .html.jsp, .md where required.
 * 
 * This allows to internally use JSP without exposing it.
 * 
 * @author Christian Kohlschütter
 */
public class HtmlJspServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private DefaultServlet defaultServlet;
  private static final String[] suffixesWithoutHtml = new String[] {".jsp", ".md"};
  private static final String[] suffixesWithHtml = new String[] {".md", ".html.jsp"};

  @Override
  public void init() throws ServletException {
    defaultServlet = (DefaultServlet) ((ServletHolder) getServletContext().getAttribute("holder."
        + DefaultServlet.class.getName())).getServlet();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    // FIXME: keep a LRU cache of recently requested paths

    String requestURI = req.getRequestURI();
    String pathInContext = requestURI.substring(req.getContextPath().length());

    if (defaultServlet.getResource(pathInContext).exists()) {
      // *.html file exists -> use DefaultServlet
      defaultServlet.service(req, resp);
      return;
    }

    String[] suffixes;
    if (pathInContext.endsWith(".html")) {
      pathInContext = pathInContext.substring(0, pathInContext.length() - ".html".length());
      suffixes = suffixesWithHtml;
    } else {
      // unexpected
      suffixes = suffixesWithoutHtml;
    }
    for (String suffix : suffixes) {
      String path = pathInContext + suffix;
      if (defaultServlet.getResource(path).exists()) {
        req.getRequestDispatcher(path).forward(req, resp);
        return;
      }
    }

    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
  }
}
