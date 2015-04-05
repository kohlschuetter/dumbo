/*
 * jabsorb - a Java to JavaScript Advanced Object Request Broker
 * http://www.jabsorb.org
 *
 * Copyright 2007-2009 The jabsorb team
 *
 * based on original code from
 * JSON-RPC-Java - a JSON-RPC to Java Bridge with dynamic invocation
 *
 * Copyright Metaparadigm Pte. Ltd. 2004.
 * Michael Clark <michael@metaparadigm.com>
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
 *
 */

package org.jabsorb.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

public class Browser implements Serializable
{
  private final static long serialVersionUID = 2;

  protected static class BrowserStore
  {

    private Set<String> userAgents = new TreeSet<String>();

    private String dataFile;

    protected BrowserStore(String suffix)
    {
      dataFile = System.getProperty("user.home") + "/.json-rpc-java-browsers-"
          + suffix + ".txt";
      try
      {
        load();
      }
      catch (IOException e)
      {
        System.out.println("BrowserStore(): " + e);
      }
    }

    protected synchronized void load() throws IOException
    {
      BufferedReader in = new BufferedReader(new FileReader(dataFile));
      String line;
      while ((line = in.readLine()) != null)
      {
        userAgents.add(line);
      }
      in.close();
    }

    protected synchronized void save() throws IOException
    {
      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(
          dataFile)));
      for (String userAgent : userAgents)
      {
        out.println(userAgent);
      }
      out.close();
    }

    protected boolean addUserAgent(String userAgent) throws IOException
    {
      if (!userAgents.contains(userAgent))
      {
        userAgents.add(userAgent);
        save();
        return true;
      }
      return false;
    }

    protected Set<String> getUserAgents()
    {
      return userAgents;
    }

  }

  private static BrowserStore passStore = new BrowserStore("pass");

  private static BrowserStore failStore = new BrowserStore("fail");

  public String userAgent;

  public boolean gotSession = false;

  public boolean firstRun = true;

  public boolean failed = false;

  public boolean passed = false;

  public boolean addNotify = false;

  /*
   * private static String makeKey() { byte b[] = new byte[8]; new
   * Random().nextBytes(b); StringBuffer sb = new StringBuffer(); for(int i=0; i
   * < 8; i++) { sb.append(b[i] & 0x0f + 'a'); sb.append((b[i] >> 4) & 0x0f +
   * 'a'); } return sb.toString(); }
   */

  public synchronized void passUserAgent() throws IOException
  {
    if (passed)
    {
      return;
    }
    System.out.println("Browser.passUserAgent(\"" + userAgent + "\")");
    addNotify = passStore.addUserAgent(userAgent);
    passed = true;
  }

  public synchronized void failUserAgent() throws IOException
  {
    if (failed)
    {
      return;
    }
    System.out.println("Browser.failUserAgent(\"" + userAgent + "\")");
    addNotify = failStore.addUserAgent(userAgent);
    failed = true;
  }

  public synchronized Set<String> getPassedUserAgents()
  {
    return passStore.getUserAgents();
  }

  public synchronized Set<String> getFailedUserAgents()
  {
    return failStore.getUserAgents();
  }
}
