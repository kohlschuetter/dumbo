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

package org.jabsorb.dict;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple Java Dict Client (RFC2229)
 */
public class DictClient implements Serializable
{
  private final static long serialVersionUID = 2;

  private final static boolean debug = false;

  private static String DEFAULT_HOST = "localhost";
  private static int DEFAULT_PORT = 2628;

  private String host;
  private int port;

  private transient List<Strategy> strategies = null;
  private transient List<Database> databases = null;

  private transient String ident = null;
  private transient Socket sock = null;
  private transient PrintWriter out = null;
  private transient BufferedReader in = null;

  public DictClient()
  {
    this.host = DEFAULT_HOST;
    this.port = DEFAULT_PORT;
  }

  public DictClient(String host)
  {
    this.host = host;
    this.port = DEFAULT_PORT;
  }

  public DictClient(String host, int port)
  {
    this.host = host;
    this.port = port;
  }

  public void setHost(String host)
  {
    if (sock != null && !this.host.equals(host))
    {
      close();
    }
    this.host = host;
  }

  public void setPort(int port)
  {
    if (sock != null && this.port != port)
    {
      close();
    }
    this.port = port;
  }

  private synchronized void connect() throws IOException, DictClientException
  {
    System.out.println("DictClient.connect: opening connection to " + host
      + ":" + port);
    sock = new Socket(host, port);
    in = new BufferedReader(new InputStreamReader(sock.getInputStream(),
      "UTF-8"));
    out = new PrintWriter(new OutputStreamWriter(sock.getOutputStream(),
      "UTF-8"));
    DictCommandResult r = new DictCommandResult(in.readLine());
    if (r.code != DictCommandResult.BANNER)
    {
      close();
      throw new DictClientException(r);
    }
    ident = r.msg;
    System.out.println("DictClient.connect: connected to " + host + ":"
      + port + " ident=\"" + ident + "\"");
  }

  @Override
  public void finalize()
  {
    close();
  }

  private synchronized String status() throws IOException,
    DictClientException
  {
    out.print("STATUS\n");
    out.flush();
    DictCommandResult r = new DictCommandResult(in.readLine());
    if (r.code != DictCommandResult.STATUS)
    {
      throw new DictClientException(r);
    }
    if (debug)
    {
      System.out.println("DictClient.status: " + r.msg);
    }
    return r.msg;
  }

  public synchronized String checkConnection() throws IOException,
    DictClientException
  {
    if (sock == null)
    {
      connect();
      return status();
    }
    try
    {
      return status();
    }
    catch (Exception e)
    {
      System.out.println("DictClient.status: Exception " + e);
      close();
      connect();
      return status();
    }
  }

  public synchronized void close()
  {
    if (sock == null)
    {
      return;
    }

    try
    {
      out.print("QUIT\n");
      out.flush();
      String line = in.readLine();
      if (line != null /* EOF */)
      {
        DictCommandResult r = new DictCommandResult(line);
        if (r.code != DictCommandResult.CLOSING_CONNECTION)
        {
          System.out.println("DictClient.close: Exception: " + r);
        }
      }
    }
    catch (IOException e)
    {
      System.out.println("DictClient.close: IOException while closing: "
        + e);
    }
    finally
    {
      try
      {
        sock.close();
      }
      catch (IOException e)
      {
        //Do nothing
      }
      sock = null;
      in = null;
      out = null;
      System.out.println("DictClient.close: connection closed");
    }
  }

  public synchronized List<Database> getDatabases() throws IOException,
    DictClientException
  {
    if (databases == null)
    {
      fetchDatabases();
    }
    return databases;
  }

  private synchronized void fetchDatabases() throws IOException,
    DictClientException
  {
    checkConnection();

    out.print("SHOW DATABASES\n");
    out.flush();
    DictCommandResult r = new DictCommandResult(in.readLine());
    if (r.code != DictCommandResult.DATABASES_PRESENT)
    {
      throw new DictClientException(r);
    }

    databases = new ArrayList<Database>();
    String line;
    while (true)
    {
      line = in.readLine();
      if (line.equals("."))
      {
        break;
      }
      String database = line.substring(0, line.indexOf(' '));
      String description = line.substring(line.indexOf('"') + 1, line
        .lastIndexOf('"'));
      databases.add(new Database(database, description));
    }

    r = new DictCommandResult(in.readLine());
    if (r.code != DictCommandResult.OKAY)
    {
      throw new DictClientException(r);
    }
  }

  public synchronized List<Strategy> getStrategies() throws IOException,
    DictClientException
  {
    if (strategies == null)
    {
      fetchStrategies();
    }
    return strategies;
  }

  private synchronized void fetchStrategies() throws IOException,
    DictClientException
  {
    checkConnection();

    out.print("SHOW STRATEGIES\n");
    out.flush();
    DictCommandResult r = new DictCommandResult(in.readLine());
    if (r.code != DictCommandResult.STRATEGIES_PRESENT)
    {
      throw new DictClientException(r);
    }

    strategies = new ArrayList<Strategy>();
    String line;
    while (true)
    {
      line = in.readLine();
      if (line.equals("."))
      {
        break;
      }
      String strategy = line.substring(0, line.indexOf(' '));
      String description = line.substring(line.indexOf('"') + 1, line
        .lastIndexOf('"'));
      strategies.add(new Strategy(strategy, description));
    }

    r = new DictCommandResult(in.readLine());
    if (r.code != DictCommandResult.OKAY)
    {
      throw new DictClientException(r);
    }
  }

  public synchronized List<Match> matchWord(String db, String strategy,
                                          String word) throws IOException, DictClientException
  {
    checkConnection();

    if (debug)
    {
      System.out.println("DictClient.matchWord(\"" + db + "\", \""
        + strategy + "\", \"" + word + "\")");
    }

    List<Match> matches = new ArrayList<Match>();

    out.print("MATCH " + db + " " + strategy + " \"" + word + "\"\n");
    out.flush();
    DictCommandResult r = new DictCommandResult(in.readLine());
    if (r.code == DictCommandResult.NO_MATCH)
    {
      return matches;
    }
    else if (r.code != DictCommandResult.MATCH_NUM_RECIEVED)
    {
      throw new DictClientException(r);
    }

    while (true)
    {
      String line = in.readLine();
      if (line.equals("."))
      {
        break;
      }
      String rDb = line.substring(0, line.indexOf(' '));
      String rWord = line.substring(line.indexOf('"') + 1, line
        .lastIndexOf('"'));
      matches.add(new Match(rDb, rWord));
    }
    r = new DictCommandResult(in.readLine());
    if (r.code == DictCommandResult.OKAY)
    {
      return matches;
    }
    throw new DictClientException(r);
  }

  public synchronized List<Definition> defineWord(String db, String word)
    throws IOException, DictClientException
  {
    checkConnection();

    if (debug)
    {
      System.out.println("DictClient.defineWord(\"" + db + "\", \""
        + word + "\")");
    }

    List<Definition> definitions = new ArrayList<Definition>();

    out.print("DEFINE " + db + " \"" + word + "\"\n");
    out.flush();
    DictCommandResult r = new DictCommandResult(in.readLine());
    if (r.code == DictCommandResult.NO_MATCH)
    {
      return definitions;
    }
    else if (r.code != DictCommandResult.DEFINE_NUM_RECIEVED)
    {
      throw new DictClientException(r);
    }

    while (true)
    {
      r = new DictCommandResult(in.readLine());
      if (r.code == DictCommandResult.OKAY)
      {
        return definitions;
      }

      int qoff;
      String line = r.msg;
      String rWord = line.substring((qoff = line.indexOf('"') + 1),
        (qoff = line.indexOf('"', qoff + 1)));
      String rDb = line.substring(qoff + 2, line.indexOf(' ', qoff + 2));
      StringBuffer def = new StringBuffer();
      while (true)
      {
        line = in.readLine();
        if (line.equals("."))
        {
          break;
        }
        def.append(line);
        def.append("\n");
      }
      definitions.add(new Definition(rDb, rWord, def.toString()));
    }
  }
}
