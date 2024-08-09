package com.kohlschutter.dumbo.api;

import java.nio.file.Path;

public class DumboTLSConfig {
  private final String hostname;
  private final int port;
  private final Path keystore;
  private final String keystorePassword;

  private DumboTLSConfig(String hostname, int port, Path keystore, String password) {
    this.hostname = hostname;
    this.port = port;
    this.keystore = keystore;
    this.keystorePassword = password;
  }

  public static DumboTLSConfig withPortAndKeystore(int port, Path keystore, String password) {
    return withPortAndKeystore(null, port, keystore, password);
  }

  public static DumboTLSConfig withPortAndKeystore(String hostname, int port, Path keystore,
      String password) {
    return new DumboTLSConfig(hostname, port, keystore, password);
  }

  public int getPort() {
    return port;
  }

  public Path getKeystorePath() {
    return keystore;
  }

  public String getKeystorePassword() {
    return keystorePassword;
  }

  public String getHostname() {
    return hostname;
  }
}
