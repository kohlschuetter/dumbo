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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map.Entry;

public class Unicode implements Serializable
{
  private final static long serialVersionUID = 2;

  static InputStream getResourceStream(String rsrcName)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    return loader.getResourceAsStream("unicode/" + rsrcName);
  }

  public static class UnicodeTest implements Serializable
  {

    private final static long serialVersionUID = 2;

    private String desc;

    private String charset;

    private String rsrc;

    private String data;

    private boolean compares = false;

    public void setDescription(String desc)
    {
      this.desc = desc;
    }

    public void setCharset(String charset)
    {
      this.charset = charset;
    }

    public void setResource(String rsrc)
    {
      this.rsrc = rsrc;
    }

    public void setData(String data)
    {
      this.data = data;
    }

    void setCompares(boolean b)
    {
      compares = b;
    }

    public String getDescription()
    {
      return desc;
    }

    public String getCharSet()
    {
      return charset;
    }

    public boolean getCompares()
    {
      return compares;
    }

    public synchronized String getData() throws IOException
    {
      if (data == null)
      {
        loadData();
      }
      return data;
    }

    protected void loadData() throws IOException
    {
      BufferedReader in = new BufferedReader(new InputStreamReader(
          getResourceStream(rsrc), charset));
      StringBuffer sb = new StringBuffer();
      String line;
      while ((line = in.readLine()) != null)
      {
        sb.append(line);
      }
      in.close();
      data = sb.toString();
    }
  }

  protected static class UnicodeTestStore implements Serializable
  {

    private final static long serialVersionUID = 2;

    private Map<String, UnicodeTest> tests = new HashMap<String, UnicodeTest>();

    private Properties testProps = new Properties();

    protected UnicodeTestStore(String indexName)
    {
      try
      {
        InputStream in = getResourceStream(indexName);
        testProps.load(in);
        in.close();
        for (Entry<Object, Object> m : testProps.entrySet())
        {
          String key = (String) m.getKey();
          String value = (String) m.getValue();
          StringTokenizer tok = new StringTokenizer(key, ".");
          String testName = tok.nextToken();
          if (!tok.hasMoreElements())
          {
            throw new Exception("invalid syntax: " + key);
          }
          String testAttr = tok.nextToken();
          UnicodeTest test = tests.get(testName);
          if (test == null)
          {
            test = new UnicodeTest();
            tests.put(testName, test);
          }
          if (testAttr.equals("description"))
          {
            test.setDescription(value);
          }
          else if (testAttr.equals("charset"))
          {
            test.setCharset(value);
          }
          else if (testAttr.equals("resource"))
          {
            test.setResource(value);
          }
          else
          {
            throw new Exception("invalid attribute: " + key);
          }
        }
      }
      catch (Exception e)
      {
        System.out.println("UnicodeTestStore(): " + e);
      }
    }

    public Map<String, UnicodeTest> getTests()
    {
      return tests;
    }
  }

  private UnicodeTestStore store = new UnicodeTestStore("00index.properties");

  public Map<String, UnicodeTest> getTests()
  {
    return store.getTests();
  }

  public Map<String,UnicodeTest>compareTests(Map<String, UnicodeTest> remoteTests) throws Exception
  {
    for (Map.Entry<String, UnicodeTest> m : remoteTests.entrySet())
    {
      String testName = m.getKey();
      UnicodeTest remoteTest = m.getValue();
      UnicodeTest localTest = store.getTests().get(testName);
      if (localTest == null)
      {
        throw new Exception("test not found");
      }
      remoteTest.setCompares(localTest.getData().equals(remoteTest.getData()));
    }
    return remoteTests;
  }
}
