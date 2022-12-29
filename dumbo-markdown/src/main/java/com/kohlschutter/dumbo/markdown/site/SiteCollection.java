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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Callable;

import com.kohlschutter.dumbo.markdown.LiquidHelper;
import com.kohlschutter.dumbo.markdown.util.PathReaderSupplier;
import com.kohlschutter.stringhold.IOExceptionHandler.ExceptionResponse;
import com.kohlschutter.stringhold.StringHolder;

/**
 * Provides a Jekyll-compatible site collection object (e.g., site.posts)
 *
 * @author Christian Kohlschütter
 */
public final class SiteCollection implements List<Object> {
  private final LiquidHelper liquid;
  private final String collectionId;
  private Iterable<PathReaderSupplier> objectSuppliers;
  private List<Object> objects;
  private final Map<String, Object> variables;
  private Integer size;
  private boolean output;

  SiteCollection(LiquidHelper liquid, Map<String, Object> collectionsConfig, String collectionId,
      Map<String, Object> variables, Iterable<PathReaderSupplier> entries) {
    this.liquid = liquid;
    this.collectionId = collectionId;
    this.variables = variables;

    this.objectSuppliers = entries;

    @SuppressWarnings("unchecked")
    Map<String, Object> collectionConfig = (Map<String, Object>) collectionsConfig.get(
        collectionId);
    if (collectionConfig != null) {
      // sort if necessary
      sortBy(collectionConfig.get("sort_by"));

      this.output = Boolean.valueOf(String.valueOf(collectionConfig.get("output")));
    }
  }

  private void populateObjects() {
    for (int i = 0, n = size(); i < n; i++) {
      get(i);
    }
  }

  public SiteCollection sortBy(Object key) {
    if (key == null) {
      return this;
    }
    populateObjects();

    Collections.sort(objects, new Comparator<Object>() {

      @SuppressWarnings("unchecked")
      @Override
      public int compare(Object o1, Object o2) {
        if (o1 instanceof Map && o2 instanceof Map) {
          Map<?, ?> m1 = (Map<?, ?>) o1;
          Map<?, ?> m2 = (Map<?, ?>) o2;

          Object k1 = m1.get(key);
          Object k2 = m2.get(key);
          if (k1 == null) {
            if (k2 == null) {
              return 0;
            } else {
              return -1;
            }
          } else if (k2 == null) {
            return 1;
          } else if (k1 instanceof Comparable) {
            return ((Comparable<Object>) k1).compareTo(k2);
          } else if (k2 instanceof Comparable) {
            return -((Comparable<Object>) k2).compareTo(k1);
          }
        }
        return 0;
      }
    });

    return this;
  }

  private Map<String, Object> loadObject(PathReaderSupplier supp, int index)
      throws FileNotFoundException, IOException {
    Map<String, Object> map = new HashMap<>();
    map.put("pin", false);
    map.put("hidden", false);
    // map.put("title", null);
    // map.put("order", null);
    // map.put("date", null);
    // map.put("url", null);
    // map.put("excerpt", null);
    map.put("last_modified_at", "2022-01-01"); // FIXME date
    map.put("previous", null);
    map.put("next", null);
    map.put("collection", collectionId);
    Map<String, Object> itemVariables = new FilterMap<String, Object>(map) {

      boolean parsedFrontMatter = false;
      Map<String, Object> iv = null;

      @Override
      public Object get(Object key) {
        if (index > 0 && "previous".equals(key)) {
          return SiteCollection.this.get(index - 1);
        } else if (index < size() - 1 && "next".equals(key)) {
          return SiteCollection.this.get(index + 1);
        } else if ("content".equals(key)) {
          // see StringHolder below
          return super.get(key);
        } else {
          Object obj = super.get(key);
          if (obj != null || (parsedFrontMatter && !super.containsKey(key))) {
            return obj;
          }

          if (!parsedFrontMatter) {
            parsedFrontMatter = true;
            try {
              iv = liquid.parseFrontMatter(supp, null, "page", () -> map);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }

          if (iv != null) {
            obj = iv.get(key);
            if (key instanceof String) {
              super.put((String) key, obj);
            }
          }

          return obj;
        }
      }
    };

    CustomSiteVariables.storePathAndFilename(supp.getRelativePath(), map);

    map.put("content", StringHolder.withSupplier(() -> liquid.prerenderLiquid(supp, variables,
        "page", () -> map), (e) -> ExceptionResponse.ILLEGAL_STATE));

    if (index > 0) {
      itemVariables.put("previous", new Callable<Object>() {

        @Override
        public Object call() throws Exception {
          return get(index - 1);
        }
      });
    }
    if (index < size() - 1) {
      itemVariables.put("next", new Callable<Object>() {

        @Override
        public Object call() throws Exception {
          return get(index + 1);
        }
      });
    }

    return itemVariables;
  }

  private Object updateObject(PathReaderSupplier supp, int index) {
    Object obj;
    try {
      obj = loadObject(supp, index);
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
    final Iterator<PathReaderSupplier> it = objectSuppliers.iterator();
    return new Iterator<Object>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public Object next() {
        int index = i++;
        PathReaderSupplier supp = it.next();
        if (objects != null && index < objects.size()) {
          Object obj = get(index);
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
    Iterator<PathReaderSupplier> it = objectSuppliers.iterator();
    PathReaderSupplier supp = null;
    while (it.hasNext()) {
      PathReaderSupplier s = it.next();
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
    final Iterator<PathReaderSupplier> it = objectSuppliers.iterator();
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
    int s = size();
    Object[] arr = new Object[s];

    Iterator<Object> it = iterator();
    for (int i = 0; i < s; i++) {
      arr[i] = it.next();
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

  public boolean isOutput() {
    return output;
  }
}