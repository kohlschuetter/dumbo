package com.kohlschutter.dumbo.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Tries to determine a hostname for localHost that is accessible from the local network.
 * 
 * @author Christian Kohlschütter
 */
public final class NetworkHostnameUtil {
  private static final Pattern PAT_CHARACTER = Pattern.compile("[a-z]");

  private NetworkHostnameUtil() {
  }

  public static String getNetworkHostname() {
    InetAddress localHost;
    try {
      localHost = Inet4Address.getLocalHost();
    } catch (UnknownHostException e) {
      localHost = Inet4Address.getLoopbackAddress();
    }
    String hostname = localHost.getHostName();

    if (!hostname.endsWith(".local")) {
      return hostname;
    }
    String unqualifiedHost = hostname.substring(0, hostname.length() - ".local".length());

    Set<Inet4Address> candidates = new LinkedHashSet<>();

    try {
      Iterator<NetworkInterface> it = NetworkInterface.getNetworkInterfaces().asIterator();
      while (it.hasNext()) {
        NetworkInterface intf = it.next();
        if (!intf.isUp() || intf.isLoopback() || intf.isVirtual() || intf.isPointToPoint()) {
          continue;
        }
        for (InterfaceAddress ia : intf.getInterfaceAddresses()) {
          InetAddress addr = ia.getAddress();
          if (!(addr instanceof Inet4Address)) {
            continue;
          }
          candidates.add(((Inet4Address) addr));
        }
      }

      for (Inet4Address addr : candidates) {
        String hn = addr.getHostName();
        if (PAT_CHARACTER.matcher(hn).find()) {
          return hn;
        }
      }

      List<String> searchDomains = getSearchDomains();
      if (!searchDomains.isEmpty()) {
        for (Inet4Address addr : candidates) {
          String hn = addr.getHostName();

          for (String sd : searchDomains) {
            try {
              InetAddress ra = InetAddress.getByName(unqualifiedHost + "." + sd);
              if (addr.equals(ra)) {
                return ra.getHostName();
              }
            } catch (UnknownHostException e) {
              continue;
            }
          }

          if (PAT_CHARACTER.matcher(hn).find()) {
            return hn;
          }
        }
      }
    } catch (SocketException e) {
      // ignore
    }
    return hostname;
  }

  private static List<String> getSearchDomains() {
    Path resolvConfFile = Path.of("/etc/resolv.conf");
    if (!Files.exists(resolvConfFile)) {
      return Collections.emptyList();
    }
    try (BufferedReader br = Files.newBufferedReader(resolvConfFile)) {
      String l;
      String searchLine = null;
      while ((l = br.readLine()) != null) {
        if (l.startsWith("search ")) {
          searchLine = l;
        }
      }
      if (searchLine != null) {
        List<String> list = Arrays.asList(searchLine.split("[ ]+"));
        list = list.subList(1, list.size());
        return list;
      }
    } catch (IOException e) {
      // ignore
    }
    return Collections.emptyList();
  }
}
