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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.kohlschutter.dumbo.markdown.util.PathReaderSupplier;
import com.vladsch.flexmark.util.ast.Document;

public final class LiquidMarkdownHelper extends MarkdownHelper {
  private final LiquidHelper liquidHelper;

  LiquidMarkdownHelper(LiquidHelper liquidHelper) {
    super();
    this.liquidHelper = liquidHelper;
  }

  /**
   * Parses the given Markdown file so it can be rendered.
   *
   * The file can have an optional front matter.
   *
   * @param relativePath The path, relative to the site root.
   * @param mdFile The markdown file.
   * @param variables The variables to use in the scope of the file.
   * @return The parsed markdown object, ready to be rendered.
   * @throws IOException on error.
   */
  public Document parseLiquidMarkdown(String relativePath, File mdFile,
      Map<String, Object> variables) throws IOException {

    return parseLiquidMarkdown(PathReaderSupplier.withContentsOf(relativePath, mdFile,
        StandardCharsets.UTF_8), (int) mdFile.length(), variables);
  }

  /**
   * Parses the given Markdown file so it can be rendered.
   *
   * The file can have an optional front matter.
   *
   * @param relativePath The path, relative to the site root.
   * @param mdPath The path to the markdown file.
   * @param variables The variables to use in the scope of the file.
   * @return The parsed markdown object, ready to be rendered.
   * @throws IOException on error.
   */
  public Document parseLiquidMarkdown(String relativePath, Path mdPath,
      Map<String, Object> variables, String type) throws IOException {
    return parseLiquidMarkdown(PathReaderSupplier.withContentsOf(type, relativePath, mdPath,
        StandardCharsets.UTF_8), mdPath == null ? 0 : (int) Files.size(mdPath), variables);
  }

  /**
   * Parses the given Markdown file so it can be rendered.
   *
   * The file can have an optional front matter.
   *
   * @param relativePath The path, relative to the site root.
   * @param mdURL The URL pointing to at the markdown source.
   * @param variables The variables to use in the scope of the file.
   * @return The parsed markdown object, ready to be rendered.
   * @throws IOException on error.
   */
  public Document parseLiquidMarkdown(String relativePath, URL mdURL, Map<String, Object> variables)
      throws IOException {

    return parseLiquidMarkdown(PathReaderSupplier.withContentsOf(relativePath, mdURL,
        StandardCharsets.UTF_8), 0, variables);
  }

  /**
   * Parses the given Markdown source so it can be rendered.
   *
   * The file can have an optional front matter.
   *
   * @param in The markdown source.
   * @param variables The variables to use in the scope of the file.
   * @return The parsed markdown object, ready to be rendered.
   * @throws IOException on error.
   */
  public Document parseLiquidMarkdown(PathReaderSupplier in, int estimatedLen,
      Map<String, Object> variables) throws IOException {
    Object liquid = getLiquidHelper().prerenderLiquid(in, estimatedLen, variables);

    return parseMarkdown(liquid);
  }

  public LiquidHelper getLiquidHelper() {
    return liquidHelper;
  }
}
