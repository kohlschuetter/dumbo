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
package com.kohlschutter.dumbo.markdown.liqp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.kohlschutter.dumbo.markdown.YAMLSupport;
import com.kohlschutter.dumbo.markdown.util.ReflectionSupplierMap;

import liqp.Template;
import liqp.TemplateContext;
import liqp.filters.Filter;
import liqp.filters.Filters;
import liqp.nodes.AtomNode;
import liqp.nodes.LNode;
import liqp.tags.Tag;

/**
 * Jekyll SEO tag filter.
 * 
 * @see <a href=
 *      "https://github.com/jekyll/jekyll-seo-tag">https://github.com/jekyll/jekyll-seo-tag</a>
 * @see <a href=
 *      "https://github.com/jekyll/jekyll-seo-tag/blob/master/lib/template.html">template</a>
 * @see <a href=
 *      "https://github.com/jekyll/jekyll-seo-tag/blob/master/lib/jekyll-seo-tag/drop.rb">variables</a>
 * @author Christian Kohlschütter
 */
public class SeoTag extends Tag {
  private static final Pattern PAT_HOMEPAGE_OR_ABOUT = Pattern.compile(
      "^/(about/)?(index.html?)?$");
  private static final String[] FORMAT_STRING_FILTERS = new String[] {
      "markdownify", "strip_html", "normalize_whitespace", "escape_once"};
  private static final String TITLE_SEPARATOR = " | ";
  private static final String VERSION = "Dumbo-1.0";
  private static final Set<String> VALID_ENTITY_TYPES = Set.of("BlogPosting", "CreativeWork");
  private static final Set<String> VALID_AUTHOR_TYPES = Set.of("Organization", "Person");

  public SeoTag() {
    super("seo");
  }

  @Override
  // FIXME incomplete implementation
  public Object render(TemplateContext context, LNode... nodes) {
    Map<String, Object> variables = new HashMap<String, Object>();

    SeoTagMap seoTagMap = new SeoTagMap(context);

    for (LNode node : nodes) {
      if (node instanceof AtomNode) {
        String v = String.valueOf(node.render(context));
        if ("title=false".equals(v)) {
          seoTagMap.put("title", null);
        } else {
          throw new IllegalStateException("Unsupported seo node value: " + node);
        }
      } else {
        throw new IllegalStateException("Unsupported seo node type: " + node.getClass() + " -  "
            + node);
      }
    }

    // String url = YAMLSupport.getVariableAsString(site, "url");
    // if (url == null) {
    // url = YAMLSupport.getVariableAsString(site, "github", "url");
    // }

    variables.put("seo_tag", seoTagMap);

    Template template;
    try {
      template = context.getParser().parse(SeoTag.class.getResourceAsStream("seo-template.html"));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return template.renderToObjectUnguarded(variables, context, true);
  }

  protected static class SeoTagMap extends ReflectionSupplierMap<Object> {
    private final TemplateContext context;

    private final Map<String, Object> site;

    private final Map<String, Object> page;

    private final Map<String, Object> paginator;

    private final String pageNumber;

    @SuppressWarnings("unchecked")
    public SeoTagMap(TemplateContext context) {
      super(Object.class);
      this.context = context;

      site = (Map<String, Object>) context.get("site");
      page = (Map<String, Object>) context.get("page");
      paginator = (Map<String, Object>) context.get("paginator");

      pageNumber = null;
      Object pageCurrent;
      if (paginator != null && (pageCurrent = paginator.get("page")) != null) {
        Objects.requireNonNull(pageCurrent); // silence warning
        // FIXME
      }
    }

    private String getString(String key) {
      Object v = get(key);
      if (v == null) {
        return null;
      } else {
        return String.valueOf(v);
      }
    }

    @ValueSupplier(key = "version")
    public String version() {
      return VERSION;
    }

    @ValueSupplier(key = "site_title")
    public String siteTitle() {
      String v = YAMLSupport.getVariableAsString(site, "title");
      if (v == null) {
        v = YAMLSupport.getVariableAsString(site, "name");
      }
      return formatString(v);
    }

    @ValueSupplier(key = "site_tagline")
    public String siteTagline() {
      return formatString(YAMLSupport.getVariableAsString(site, "tagline"));
    }

    @ValueSupplier(key = "site_description")
    public String siteDescription() {
      return formatString(YAMLSupport.getVariableAsString(site, "description"));
    }

    @ValueSupplier(key = "page_title")
    public String pageTitle() {
      String v = formatString(YAMLSupport.getVariableAsString(page, "title"));
      if (v == null) {
        v = getString("site_title");
      }
      return v;
    }

    @ValueSupplier(key = "site_tagline_or_description")
    public String siteTaglineOrDescription() {
      String s;
      s = getString("site_tagline");
      if (s == null) {
        s = getString("site_description");
      }
      return s;
    }

    @ValueSupplier(key = "title")
    public String title() {
      String siteTitle = getString("site_title");
      String pageTitle = getString("page_title");
      String siteDescription = getString("site_description");

      String v;
      if (siteTitle != null && !siteTitle.equals(pageTitle)) {
        if (pageTitle == null) {
          v = siteTitle;
        } else {
          v = pageTitle + TITLE_SEPARATOR + siteTitle;
        }
      } else if (siteDescription != null && siteTitle != null) {
        v = siteTitle + TITLE_SEPARATOR + get("site_tagline_or_description");
      } else if (pageTitle == null) {
        v = getString("site_tagline");
      } else {
        v = pageTitle;
      }
      if (pageNumber != null) {
        v = pageNumber + v;
      }

      return v;
    }

    @ValueSupplier(key = "description")
    public String description() {
      String v;
      v = YAMLSupport.getVariableAsString(page, "description");
      if (v != null) {
        return formatString(v);
      }
      v = YAMLSupport.getVariableAsString(page, "excerpt");
      if (v != null) {
        return formatString(v);
      }
      return getString("site_description");
    }

    @ValueSupplier(key = "author")
    public Object author() {
      return site.get("author"); // FIXME get author information from data; yaml header
    }

    @ValueSupplier(key = "json_ld")
    public Object jsonLd() {
      return new JsonLdMap(this);
    }

    // FIXME implement: name, image, links, logo

    @ValueSupplier(key = "date_modified")
    public String dateModified() {
      String v;
      v = YAMLSupport.getVariableAsString(page, "seo", "date_modified");
      if (v == null) {
        v = YAMLSupport.getVariableAsString(page, "last_modified_at");
        if (v == null) {
          v = YAMLSupport.getVariableAsString(page, "date");
        }
      }
      if (v != null) {
        v = String.valueOf(context.getParser().filters.get("date_to_xmlschema").apply(v, context));
      }
      return v;
    }

    @ValueSupplier(key = "date_published")
    public String datePublished() {
      String v = YAMLSupport.getVariableAsString(page, "date");

      if (v != null) {
        v = String.valueOf(context.getParser().filters.get("date_to_xmlschema").apply(v, context));
      }
      return v;
    }

    @ValueSupplier(key = "type")
    public String type() {
      String v;
      v = YAMLSupport.getVariableAsString(page, "seo", "type");
      if (v != null) {
        return v;
      } else if (isHomepageOrAbout()) {
        return "WebSite";
      } else if (page.containsKey("date")) {
        return "BlogPosting";
      } else {
        return "WebPage";
      }
    }

    @ValueSupplier(key = "page_lang")
    public String pageLang() {
      String v = YAMLSupport.getVariableAsString(page, "lang");
      if (v == null) {
        v = YAMLSupport.getVariableAsString(site, "lang");
        if (v == null) {
          v = "en_US";
        }
      }
      return v;
    }

    @ValueSupplier(key = "page_locale")
    public String pageLocale() {
      String v = YAMLSupport.getVariableAsString(page, "locale");
      if (v == null) {
        v = YAMLSupport.getVariableAsString(site, "locale");
        if (v == null) {
          v = getString("page_lang");
        }
      }
      v = v.replace('-', '_');
      return v;
    }

    @ValueSupplier(key = "canonical_url")
    public String canonicalUrl() {
      String v = YAMLSupport.getVariableAsString(page, "canonical_url");
      if (v == null || v.isEmpty()) {
        v = YAMLSupport.getVariableAsString(page, "url");
        Filter absoluteUrlFilter = context.getParser().filters.get("absolute_url");
        v = String.valueOf(absoluteUrlFilter.apply(v, context)).replaceFirst("/index\\.html$", "/");
      }
      return v;
    }

    private String formatString(Object o) {
      return SeoTag.formatString(context, o);
    }

    public boolean isHomepageOrAbout() {
      return SeoTag.isHomepageOrAbout(String.valueOf(page.get("url")));
    }
  }

  protected static class JsonLdMap extends ReflectionSupplierMap<Object> {
    private final SeoTagMap seoTagMap;

    public JsonLdMap(SeoTagMap seoTagMap) {
      super(Object.class);
      this.seoTagMap = seoTagMap;
    }

    @ValueSupplier(key = "@context")
    public String context() {
      return "https://schema.org";
    }

    @ValueSupplier(key = "name")
    public String name() {
      return seoTagMap.getString("name");
    }

    @ValueSupplier(key = "description")
    public String description() {
      return seoTagMap.getString("description");
    }

    @ValueSupplier(key = "url")
    public String url() {
      return seoTagMap.getString("canonical_url");
    }

    @ValueSupplier(key = "headline")
    public String headline() {
      return seoTagMap.getString("page_title");
    }

    @ValueSupplier(key = "dateModified")
    public String dateModified() {
      return seoTagMap.getString("date_modified");
    }

    @ValueSupplier(key = "datePublished")
    public String datePublished() {
      return seoTagMap.getString("date_published");
    }

    @ValueSupplier(key = "sameAs")
    public Object sameAs() {
      return seoTagMap.get("links");
    }

    @ValueSupplier(key = "logo")
    public String logo() {
      return seoTagMap.getString("logo");
    }

    @ValueSupplier(key = "@type")
    public String type() {
      return seoTagMap.getString("type");
    }

    @ValueSupplier(key = "author")
    public Object author() {
      String name = YAMLSupport.getVariableAsString(seoTagMap, "author", "name");
      if (name == null) {
        return null;
      }

      String authorType = YAMLSupport.getVariableAsString(seoTagMap, "author", "type");
      if (authorType != null && !VALID_AUTHOR_TYPES.contains(authorType)) {
        return null;
      }

      return Map.of(//
          "@type", authorType == null ? "Person" : authorType, //
          "name", name, //
          "url", YAMLSupport.getVariableAsString(seoTagMap, "author", "url") //
      );
    }

    // FIXME add image

    @ValueSupplier(key = "publisher")
    public Object publisher() {
      String logo = seoTagMap.getString("logo");
      if (logo == null) {
        return null;
      }

      return Map.of(//
          "@type", "Organization", //
          "logo", Map.of(//
              "@type", "ImageObject", //
              "url", logo //
          ), //
          "name", YAMLSupport.getVariableAsString(seoTagMap, "author", "name"));
    }

    @ValueSupplier(key = "mainEntityOfPage")
    public Object mainEntityOfPage() {
      Object type = get("@type");
      if (type == null || !VALID_ENTITY_TYPES.contains(type)) {
        return null;
      }
      return Map.of(//
          "@type", "WebPage", //
          "@id", seoTagMap.getString("canonical_url") //
      );
    }
  }

  private static boolean isHomepageOrAbout(String s) {
    return PAT_HOMEPAGE_OR_ABOUT.matcher(s).find();
  }

  private static String formatString(TemplateContext context, Object v) {
    if (v == null) {
      return null;
    }
    Filters filters = context.getParser().filters;

    for (String fn : FORMAT_STRING_FILTERS) {
      Filter f = filters.get(fn);
      if (f == null) {
        throw new IllegalStateException("Could not find filter: " + fn + " in " + filters);
      }
      v = f.apply(v, context);
    }

    return v == null ? null : String.valueOf(v);
  }
}
