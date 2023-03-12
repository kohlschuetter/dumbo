package com.kohlschutter.dumbo.markdown.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.kohlschutter.dumbo.markdown.site.FilterMap;

public class SupplierMap<K, V> extends FilterMap<K, V> {
  private final Map<K, Object> map;

  @FunctionalInterface
  private interface MapSupplier<K, V> extends Function<K, V> {
  }

  @SuppressWarnings("unchecked")
  public SupplierMap() {
    super(new LinkedHashMap<K, V>());
    this.map = (Map<K, Object>) getMap();
  }

  @Override
  public boolean containsValue(Object value) {
    for (V v : values()) {
      if (Objects.equals(v, value)) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public V get(Object key) {
    Object obj = map.get(key);
    if (obj instanceof MapSupplier) {
      K k = (K) key;
      V obj2 = unwrap(k, obj);
      getMap().put(k, obj2);
      return obj2;
    } else {
      return (V) obj;
    }
  }

  @Override
  public V put(K key, V value) {
    return unwrap(key, super.put(key, value));
  }

  @SuppressWarnings({"unchecked"})
  private V unwrap(K key, Object obj) {
    if (obj instanceof MapSupplier) {
      obj = ((MapSupplier<K, V>) obj).apply(key);
    }
    return (V) obj;
  }

  public V putSupplied(K key, Supplier<V> value) {
    MapSupplier<K, V> ms = (k) -> value.get();

    return unwrap(key, map.put(key, ms));
  }

  public V putSupplied(K key, Function<K, V> value) {
    MapSupplier<K, V> ms = (k) -> value.apply(k);

    return unwrap(key, map.put(key, ms));
  }

  @SuppressWarnings("unchecked")
  @Override
  public Collection<V> values() {
    ArrayList<Object> values = new ArrayList<Object>(super.entrySet());
    for (int i = 0, n = values.size(); i < n; i++) {
      Entry<K, Object> en = (Entry<K, Object>) values.get(i);
      Object obj = en.getValue();
      if (obj instanceof MapSupplier) {
        obj = unwrap(en.getKey(), obj);
      }
      values.set(i, obj);
    }
    return (ArrayList<V>) (ArrayList<?>) values;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<Entry<K, V>> entrySet() {
    LinkedHashSet<Entry<K, ?>> set = new LinkedHashSet<>(super.entrySet());
    for (Entry<K, ?> en : set) {
      Object obj = en.getValue();
      if (obj instanceof MapSupplier) {
        obj = unwrap(en.getKey(), obj);
        ((Entry<K, Object>) en).setValue(obj);
      }
    }
    return (Set<Entry<K, V>>) (Set<?>) set;
  }
}
