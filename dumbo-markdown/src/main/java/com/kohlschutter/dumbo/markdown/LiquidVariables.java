/*
 * dumbo-markdown
 *
 * Copyright 2022,2023 Christian Kohlschütter
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
package com.kohlschutter.dumbo.markdown;

public final class LiquidVariables {
  public static final String DUMBO = "dumbo";
  public static final String DUMBO_HTMLHEAD = "htmlHead";
  public static final String DUMBO_HTMLBODYTOP = "htmlBodyTop";
  public static final String DUMBO_STATE_TL = " tlState";

  public static final String SITE = "site";
  public static final String SITE_DATA = "data";
  public static final String SITE_TAGS = "tags";
  public static final String SITE_CATEGORIES = "categories";
  public static final String SITE_COLLECTIONS = "collections";
  public static final String SITE_POSTS = "posts";

  public static final String JEKYLL = "jekyll";

  public static final String PAGE = "page";
  public static final String PAGE_CONTENT = "content";
  public static final String PAGE_LAYOUT = "layout";
  public static final String PAGE_TAGS = "tags";
  public static final String PAGE_CATEGORIES = "categories";
  public static final String PAGE_PERMALINK = "permalink";
  public static final String PAGE_URL = "url";
  public static final String PAGE_COLLECTION = "collection";
  public static final String PAGE_TYPE = "type";
  public static final String PAGE_NAME = "name";
  public static final String PAGE_TITLE = "title";
  public static final String PAGE_LAST_MODIFIED_AT = "last_modified_at";

  public static final String PAGINATOR = "paginator";

  public static final String JEKYLLARCHIVES = "jekyll-archives";
  public static final String JEKYLLARCHIVES_ENABLED = "enabled";
  public static final String JEKYLLARCHIVES_LAYOUT = "layout";
  public static final String JEKYLLARCHIVES_LAYOUTS = "layouts";
  public static final String JEKYLLARCHIVES_PERMALINKS = "permalinks";

  private LiquidVariables() {
  }
}