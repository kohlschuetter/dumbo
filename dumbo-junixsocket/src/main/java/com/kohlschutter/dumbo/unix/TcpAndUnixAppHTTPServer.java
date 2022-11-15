package com.kohlschutter.dumbo.unix;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.kohlschutter.dumbo.AppHTTPServer;
import com.kohlschutter.dumbo.ServerApp;

/**
 * An {@link AppHTTPServer} that binds both on a TCP port as well as on an AF_UNIX socket address.
 *
 * @author Christian Kohlschütter
 */
public class TcpAndUnixAppHTTPServer extends UnixAppHTTPServer {
  public TcpAndUnixAppHTTPServer(ServerApp app) throws IOException {
    super(app);
  }

  public TcpAndUnixAppHTTPServer(ServerApp app, String path, URL webappBaseURL) throws IOException {
    super(app, path, webappBaseURL);
  }

  public TcpAndUnixAppHTTPServer(ServerApp app, String path) throws IOException {
    super(app, path);
  }

  public TcpAndUnixAppHTTPServer(ServerApp app, URL webappBaseURL) throws IOException {
    super(app, webappBaseURL);
  }

  @Override
  protected Connector[] initConnectors(Server targetServer) throws IOException {
    ServerConnector tcpConn = initDefaultTCPConnector(targetServer);
    return new Connector[] {
        tcpConn, initUnixConnector(targetServer, AFUNIXSocketAddress.of(new File("/tmp/dumbo-"
            + tcpConn.getPort() + ".sock")))};
  }
}
