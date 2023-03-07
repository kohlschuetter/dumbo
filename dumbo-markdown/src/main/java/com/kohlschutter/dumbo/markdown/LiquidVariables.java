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

public interface LiquidVariables {
  String DUMBO = "dumbo";
  String DUMBO_HTMLHEAD = "htmlHead";
  String DUMBO_HTMLBODYTOP = "htmlBodyTop";
  String DUMBO_STATE_TL = " tlState";

  String SITE = "site";
  String SITE_DATA = "data";
  String SITE_TAGS = "tags";
  String SITE_CATEGORIES = "categories";
  String SITE_COLLECTIONS = "collections";
  String SITE_POSTS = "posts";

  String JEKYLL = "jekyll";

  String PAGE = "page";
  String PAGE_LAYOUT = "layout";
  String PAGE_TAGS = "tags";
  String PAGE_CATEGORIES = "categories";
  String PAGE_PERMALINK = "permalink";
  String PAGE_URL = "url";
  String PAGE_COLLECTION = "collection";
  String PAGE_TYPE = "type";
  String PAGE_NAME = "name";
  String PAGE_TITLE = "title";

  String PAGINATOR = "paginator";

  String JEKYLLARCHIVES = "jekyll-archives";
  String JEKYLLARCHIVES_ENABLED = "enabled";
  String JEKYLLARCHIVES_LAYOUT = "layout";
  String JEKYLLARCHIVES_LAYOUTS = "layouts";
  String JEKYLLARCHIVES_PERMALINKS = "permalinks";

}