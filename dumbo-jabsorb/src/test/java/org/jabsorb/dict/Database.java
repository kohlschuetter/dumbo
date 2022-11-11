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
 */
package org.jabsorb.dict;

import java.io.Serializable;

/**
 * Simple Java Dict Client (RFC2229)
 */
public class Database implements Serializable {
  private final static long serialVersionUID = 2;

  private String database;

  private String description;

  public String getDatabase() {
    return database;
  }

  public String getDescription() {
    return description;
  }

  public Database(String database, String description) {
    this.database = database;
    this.description = description;
  }

  @Override
  public String toString() {
    return "database: " + database + " \"" + description + "\"";
  }
}
