package com.kohlschutter.dumbo.jek.liqp.tags;

import liqp.TemplateContext;
import liqp.nodes.LNode;
import liqp.tags.Tag;

public class AssetPath extends Tag {
  
  public AssetPath() {
    super("asset_path");
  }

  @Override
  public Object render(TemplateContext context, LNode... nodes) {

    System.out.println("FIXME: ASSET PATH, nodes: " + nodes.length);
    if (nodes.length == 1) {
      String v = asString(nodes[0].render(context), context);
      System.out.println("ASSET PATH node[0]: " + v);
      return "/assets/posts/2022/10/28/linux-nanopi-r4s/" +v;
    }

    return "";
  }
}
