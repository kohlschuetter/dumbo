/*
 * Copyright 2022,2023 Christian Kohlschütter
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
package com.kohlschutter.dumbo.api;

import java.nio.file.Path;

public final class DumboTLSConfig {
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
