package com.kohlschutter.dumbo.liqp;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.kohlschutter.dumbo.ServerApp;

import liqp.Template;
import liqp.TemplateContext;
import liqp.nodes.LNode;
import liqp.parser.Flavor;
import liqp.tags.Tag;

public class DumboInclude extends Tag {
  public static String DEFAULT_EXTENSION = ".liquid";

  DumboInclude() {
    super("include");
  }

  @Override
  public Object render(TemplateContext context, LNode... nodes) {
    @SuppressWarnings("unchecked")
    ServerApp app = (ServerApp) ((Map<String, Object>) context.getVariables().get("dumbo")).get(
        ".app");
    try {
      String includeResource = super.asString(nodes[0].render(context), context);
      if (includeResource.isEmpty()) {
        throw new FileNotFoundException("Can't include " + nodes[0] + " (empty string)");
      }
      if (includeResource.indexOf('.') == 0) {
        includeResource += DEFAULT_EXTENSION;
      }

      URL resource = app.getResource("markdown/_includes/" + includeResource);
      if (resource == null) {
        throw new FileNotFoundException("Can't include " + includeResource);
      }

      String str;
      try (InputStream in = resource.openStream()) {
        str = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
      Template template = Template.parse(str, context.parseSettings, context.renderSettings);

      if (nodes.length > 1) {
        if (context.parseSettings.flavor != Flavor.JEKYLL) {
          // check if there's a optional "with expression"
          Object value = nodes[1].render(context);
          context.put(includeResource, value);
        } else {
          Map<String, Object> variables = new HashMap<String, Object>();
          for (int i = 1, n = nodes.length; i < n; i++) {
            @SuppressWarnings("unchecked")
            Map<String, Object> var = (Map<String, Object>) nodes[i].render(context);
            variables.putAll(var);
          }
          return template.renderUnguarded(variables, context, true);
        }
      }

      return template.renderUnguarded(context);
    } catch (Exception e) {
      if (context.renderSettings.showExceptionsFromInclude) {
        throw new RuntimeException("problem with evaluating include", e);
      } else {
        return "";
      }
    }
  }
}
