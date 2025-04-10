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
package com.kohlschutter.dumbo.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;

@SuppressFBWarnings("WEAK_MESSAGE_DIGEST_SHA1")
public final class NameObfuscator {
  private static final ThreadLocal<MessageDigest> TL_SHA1 = SuppliedThreadLocal.of(() -> {
    try {
      return MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  });

  private NameObfuscator() {
    throw new IllegalStateException("No instances");
  }

  public static String obfuscate(String s) {
    return Base64.getUrlEncoder().encodeToString(TL_SHA1.get().digest(s.getBytes(
        StandardCharsets.UTF_8)));
  }
}
