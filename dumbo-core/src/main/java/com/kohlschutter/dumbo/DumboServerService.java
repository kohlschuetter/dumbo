package com.kohlschutter.dumbo;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.kohlschutter.dumbo.annotations.DumboService;
import com.kohlschutter.dumbo.api.DumboApplication;
import com.kohlschutter.dumbo.api.DumboServiceProviders;
import com.kohlschutter.dumborb.client.Client;
import com.kohlschutter.dumborb.client.SystemObject;

public class DumboServerService implements DumboServiceProviders {
  private final AppHTTPServer server;
  private final Client client;
  private final Set<String> knownRpcServices;

  public DumboServerService(Class<? extends DumboApplication> applicationClass) throws IOException,
      InterruptedException {
    server = new AppHTTPServer(new ServerApp(applicationClass)).start();

    this.client = server.newJsonRpcClient();
    this.knownRpcServices = retrieveKnownRpcServices(client);
  }

  private static Set<String> retrieveKnownRpcServices(Client client) {
    Set<String> services = new HashSet<>();
    SystemObject system = client.openProxy("system", SystemObject.class);
    for (String m : system.listMethods()) {
      int lastDot = m.lastIndexOf('.');
      if (lastDot == -1) {
        continue;
      }
      services.add(m.substring(0, lastDot));
    }
    return services;
  }

  @Override
  public <T> Collection<T> getDumboServices(Class<T> clazz) {
    DumboService ds = clazz.getAnnotation(DumboService.class);
    String rpcName = ds.rpcName();
    if (rpcName == null || rpcName.isEmpty()) {
      rpcName = clazz.getName();
    }

    return Stream.of(server.getDumboService(clazz), knownRpcServices.contains(rpcName) ? client
        .openProxy(rpcName, clazz) : null).filter((p) -> p != null).toList();
  }
}
