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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import liqp.TemplateContext;
import liqp.filters.Filter;

/**
 * Data To JSON.
 * 
 * Convert Hash or Array to JSON.
 * 
 * @author Christian Kohlschütter
 */
public class JsonifyFilter extends Filter {
  private final ObjectWriter ow = new ObjectMapper().setSerializationInclusion(Include.NON_ABSENT)
      .writer().withFeatures(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

  public JsonifyFilter() {
    super("jsonify");
  }

  @Override
  public Object apply(Object value, TemplateContext context, Object... params) {
    try {
      return ow.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
