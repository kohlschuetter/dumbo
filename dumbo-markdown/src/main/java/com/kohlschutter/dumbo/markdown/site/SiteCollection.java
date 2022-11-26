/*
 * dumbo-markdown
 *
 * Copyright 2022 Christian Kohlschütter
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
package com.kohlschutter.dumbo.markdown.site;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.kohlschutter.dumbo.markdown.LiquidHelper;
import com.kohlschutter.stringhold.IOSupplier;

/**
 * Provides a Jekyll-compatible site collection object (e.g., site.posts)
 *
 * @author Christian Kohlschütter
 */
public final class SiteCollection implements List<Object> {
  private final LiquidHelper liquid;
  private Iterable<IOSupplier<Reader>> entries;
  private List<Object> objects;
  private final Map<String, Object> variables;
  private Integer size;

  SiteCollection(LiquidHelper liquid, Map<String, Object> variables,
      Iterable<IOSupplier<Reader>> entries) {
    this.liquid = liquid;
    this.variables = variables;

    this.entries = entries;
  }

  private Object loadObject(IOSupplier<Reader> supp) throws FileNotFoundException, IOException {
    Map<String, Object> pageVariables = new HashMap<>();
    Object content = liquid.prerenderLiquid(supp, variables, () -> pageVariables);
    pageVariables.put("content", content);
    return pageVariables;
  }

  private Object updateObject(IOSupplier<Reader> supp, int index) {
    Object obj;
    try {
      obj = loadObject(supp);
    } catch (IOException e) {
      // FIXME IOException
      e.printStackTrace();
      obj = Collections.emptyMap();
    }

    if (objects == null) {
      objects = new ArrayList<>(index);
    }

    // pad objects
    for (int i = 0, n = (index + 1 - objects.size()); i < n; i++) {
      objects.add(null);
    }

    objects.set(index, obj);
    return obj;
  }

  @Override
  public Iterator<Object> iterator() {
    final Iterator<IOSupplier<Reader>> it = entries.iterator();
    return new Iterator<Object>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public Object next() {
        int index = i++;
        IOSupplier<Reader> supp = it.next();
        if (objects != null && index < objects.size()) {
          Object obj = objects.get(index);
          if (obj != null) {
            return obj;
          }
        }

        return updateObject(supp, index);
      }
    };
  }

  @Override
  public Object get(int index) {
    if (objects != null && index < objects.size()) {
      Object obj = objects.get(index);
      if (obj != null) {
        return obj;
      }
    }

    int i = 0;
    Iterator<IOSupplier<Reader>> it = entries.iterator();
    IOSupplier<Reader> supp = null;
    while (it.hasNext()) {
      IOSupplier<Reader> s = it.next();
      if (i++ == index) {
        supp = s;
        break;
      }
    }

    if (supp == null) {
      // out of bounds
      return Collections.emptyMap();
    }

    return updateObject(supp, index);
  }

  @Override
  public int size() {
    if (size != null) {
      return size;
    }
    final Iterator<IOSupplier<Reader>> it = entries.iterator();
    int s = 0;
    while (it.hasNext()) {
      it.next();
      s++;
    }

    return (size = s);
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean contains(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object[] toArray() {
    // FIXME should not be necessary
    int s = size();
    Object[] arr = new Object[s];

    Iterator<Object> it = iterator();
    for (int i = 0; i < s; i++) {
      arr[i++] = it.next();
    }
    return arr;
  }

  @Override
  public <T> T[] toArray(T[] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(Object e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends Object> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(int index, Collection<? extends Object> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object set(int index, Object element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int index, Object element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int indexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int lastIndexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<Object> listIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<Object> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Object> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }
}