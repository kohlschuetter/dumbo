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

/**
 * Simple Java Dict Client (RFC2229)
 */
public class DictCommandResult
{
  protected int code;

  protected String msg;

  public int getCode()
  {
    return code;
  }

  public String getMessage()
  {
    return msg;
  }

  public static int INTERNAL_SOCKET_EOF = 900;
  public static int INTERNAL_STATUS_PARSE_ERROR = 901;
  public static int DATABASES_PRESENT = 110;
  public static int STRATEGIES_PRESENT = 111;
  public static int DEFINE_NUM_RECIEVED = 150;
  public static int DEFINE_RESULT = 151;
  public static int MATCH_NUM_RECIEVED = 152;
  public static int STATUS = 210;
  public static int BANNER = 220;
  public static int OKAY = 250;
  public static int CLOSING_CONNECTION = 221;
  public static int TEMP_UNAVAILABLE = 420;
  public static int INVALID_DATABASE = 550;
  public static int INVALID_STRATEGY = 551;
  public static int NO_MATCH = 552;

  protected DictCommandResult(String s)
  {
    if (s == null)
    {
      code = INTERNAL_SOCKET_EOF;
      msg = "Connection closed";
      return;
    }
    try
    {
      code = Integer.parseInt(s.substring(0, 3));
      msg = s.substring(4, s.length());
    }
    catch (Exception e)
    {
      code = INTERNAL_STATUS_PARSE_ERROR;
      msg = "Can't parse status line";
    }
  }
  @Override
  public String toString()
  {
    return "code=" + code + " msg=\"" + msg + "\"";
  }
}
