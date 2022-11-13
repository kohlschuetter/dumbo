/*
 * Copyright 2022 Christian Kohlschütter
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
package com.kohlschutter.dumbo;

/**
 * Provides simple means of communication between the JavaScript and the Java parts of the app.
 */
public interface AppControlService {
  /**
   * Called by when the app (browser window) has been loaded.
   *
   * @return A unique ID for the current instance of the app.
   */
  String notifyAppLoaded();

  /**
   * Called when the app (JavaScript page) is unloading / being closed.
   *
   * @param appId The unique ID that was returned by {@link #notifyAppLoaded()} for this browser
   *          window.
   */
  void notifyAppUnload(String appId);
}
