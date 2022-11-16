package com.kohlschutter.dumbo.unix;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.jetty.AFSocketServerConnector;

import com.kohlschutter.dumbo.AppHTTPServer;
import com.kohlschutter.dumbo.ServerApp;

/**
 * An {@link AppHTTPServer} that only binds on an AF_UNIX socket address.
 *
 * @author Christian Kohlschütter
 */
public class UnixAppHTTPServer extends AppHTTPServer {
  public UnixAppHTTPServer(ServerApp app) throws IOException {
    super(app);
  }

  public UnixAppHTTPServer(ServerApp app, String path, URL webappBaseURL) throws IOException {
    super(app, path, webappBaseURL);
  }

  public UnixAppHTTPServer(ServerApp app, String path) throws IOException {
    super(app, path);
  }

  public UnixAppHTTPServer(ServerApp app, URL webappBaseURL) throws IOException {
    super(app, webappBaseURL);
  }

  @Override
  protected Connector[] initConnectors(Server targetServer) throws IOException {
    return new Connector[] {initUnixConnector(targetServer)};
  }

  protected Connector initUnixConnector(Server targetServer) throws IOException {
    return initUnixConnector(targetServer, null);
  }

  protected Connector initUnixConnector(Server targetServer, AFUNIXSocketAddress address)
      throws IOException {

    AFSocketServerConnector unixConnector = new AFSocketServerConnector(targetServer,
        newHttpConnectionFactory());
    if (address == null) {
      address = AFUNIXSocketAddress.of(new File("/tmp/dumbo.socket"));
    }
    unixConnector.setListenSocketAddress(address);
    unixConnector.setAcceptQueueSize(128);
    unixConnector.setMayStopServerForce(true);

    return unixConnector;
  }
}
