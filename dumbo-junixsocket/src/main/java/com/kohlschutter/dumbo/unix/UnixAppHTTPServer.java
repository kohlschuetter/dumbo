package com.kohlschutter.dumbo.unix;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.jetty.AFSocketServerConnector;

import com.kohlschutter.dumbo.AppHTTPServer;
import com.kohlschutter.dumbo.ServerApp;

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
    AFSocketServerConnector unixConnector = new AFSocketServerConnector(targetServer,
        new HttpConnectionFactory());
    unixConnector.setListenSocketAddress(AFUNIXSocketAddress.of(new File("/tmp/dumbo.socket")));
    unixConnector.setAcceptQueueSize(128);
    unixConnector.setMayStopServer(true);

    return new Connector[] {unixConnector};
  }
}
