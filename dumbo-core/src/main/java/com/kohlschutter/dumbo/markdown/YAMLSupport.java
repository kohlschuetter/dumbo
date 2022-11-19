package com.kohlschutter.dumbo.markdown;

import java.util.Collection;
import java.util.Map;

public class YAMLSupport {

  public static String getVariableAsString(Map<String, Object> variables, String... pathElements) {
    Object obj = variables;
    for (String pathElement : pathElements) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) obj;
      obj = map.get(pathElement);
      if (obj == null) {
        return null;
      }
    }
    boolean loop;
    do {
      loop = false;
      if (obj instanceof Collection) {
        obj = ((Collection<?>) obj).iterator().next();
        loop = true;
      }
      if (obj instanceof Map) {
        obj = ((Map<?, ?>) obj).values();
        loop = true;
      }
      if (obj == null) {
        return null;
      }
    } while (loop);
    return obj.toString();
  }
}
