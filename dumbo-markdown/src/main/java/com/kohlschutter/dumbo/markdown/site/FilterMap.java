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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public abstract class FilterMap<K, V> implements Map<K, V> {
  private final Map<K, V> map;

  protected FilterMap(Map<K, V> map) {
    this.map = map;
  }

  protected Map<K, V> getMap() {
    return map;
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return map.get(key);
  }

  @Override
  public V put(K key, V value) {
    return map.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return map.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    map.putAll(m);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  @Override
  public Collection<V> values() {
    return map.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return map.entrySet();
  }

  public static abstract class ReadOnlyFilterMap<K, V> extends FilterMap<K, V> {

    protected ReadOnlyFilterMap(Map map) {
      super(map);
    }

    @Override
    public V put(K key, V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
      return Collections.unmodifiableSet(super.keySet());
    }

    @Override
    public Collection<V> values() {
      return Collections.unmodifiableCollection(super.values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return Collections.unmodifiableSet(super.entrySet());
    }

  }
}
