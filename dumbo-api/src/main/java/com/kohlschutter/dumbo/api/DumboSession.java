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

import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;

import com.kohlschutter.dumbo.exceptions.NoSessionException;

public abstract class DumboSession {
  private static final ThreadLocal<DumboSession> TL_DUMBO_SESSION = new ThreadLocal<>();

  protected DumboSession() {
  }

  public static DumboSession getSession() {
    DumboSession session = getSessionIfExists();
    if (session == null) {
      throw new NoSessionException();
    } else {
      return session;
    }
  }

  public static DumboSession getSessionIfExists() {
    return TL_DUMBO_SESSION.get();
  }

  public static void setSession(DumboSession session) {
    TL_DUMBO_SESSION.set(session);
  }

  public static void removeSession() {
    TL_DUMBO_SESSION.remove();
  }

  public abstract Console getConsole();

  public abstract void invalidate();

  public final <@NonNull T> T getOrCreatePageAttribute(Class<T> key, Supplier<T> attrSupplier) {
    return getOrCreatePageAttribute(key.getName(), attrSupplier);
  }

  public final <@NonNull T> T getOrCreatePageAttribute(Class<T> key,
      Function<String, T> attrSupplier) {
    return getOrCreatePageAttribute(key.getName(), attrSupplier);
  }

  public final <@NonNull T> T getOrCreatePageAttribute(String key, Supplier<T> attrSupplier) {
    return getOrCreatePageAttribute(key, (k) -> attrSupplier.get());
  }

  public abstract <@NonNull T> T getOrCreatePageAttribute(String key,
      Function<String, T> attrSupplier);

  public abstract void setPageAttribute(String key, Object val);
}
