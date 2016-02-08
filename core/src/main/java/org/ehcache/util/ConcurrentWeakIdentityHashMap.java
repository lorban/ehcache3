/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehcache.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Alex Snaps
 */
public class ConcurrentWeakIdentityHashMap<K, V> implements ConcurrentMap<K, V> {

  private final ConcurrentMap<WeakReference<K>, V> map = new ConcurrentHashMap<WeakReference<K>, V>();
  private final ReferenceQueue<K> queue = new ReferenceQueue<K>();

  @Override
  public V putIfAbsent(final K key, final V value) {
    purgeKeys();
    return map.putIfAbsent(newKey(key), value);
  }

  @Override
  public boolean remove(final Object key, final Object value) {
    purgeKeys();
    return map.remove(new WeakReference<K>((K) key, null), value);
  }

  @Override
  public boolean replace(final K key, final V oldValue, final V newValue) {
    purgeKeys();
    return map.replace(newKey(key), oldValue, newValue);
  }


  @Override
  public V replace(final K key, final V value) {
    purgeKeys();
    return map.replace(newKey(key), value);
  }

  @Override
  public int size() {
    purgeKeys();
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    purgeKeys();
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(final Object key) {
    purgeKeys();
    return map.containsKey(new WeakReference<K>((K) key, null));
  }

  @Override
  public boolean containsValue(final Object value) {
    purgeKeys();
    return map.containsValue(value);
  }

  @Override
  public V get(final Object key) {
    purgeKeys();
    return map.get(new WeakReference<K>((K) key, null));
  }

  @Override
  public V put(final K key, final V value) {
    purgeKeys();
    return map.put(newKey(key), value);
  }

  @Override
  public V remove(final Object key) {
    purgeKeys();
    return map.remove(new WeakReference<K>((K) key, null));
  }

  @Override
  public void putAll(final Map<? extends K, ? extends V> m) {
    purgeKeys();
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      map.put(newKey(entry.getKey()), entry.getValue());
    }
  }

  @Override
  public void clear() {
    purgeKeys();
    map.clear();
  }

  @Override
  public Set<K> keySet() {
    return new AbstractSet<K>() {
      @Override
      public Iterator<K> iterator() {
        purgeKeys();
        final Iterator<WeakReference<K>> iterator = map.keySet().iterator();
        final AtomicReference<K> ref = new AtomicReference<K>();
        advance(iterator, ref);

        return new Iterator<K>() {
          @Override
          public boolean hasNext() {
            return ref.get() != null;
          }

          @Override
          public K next() {
            K k = ref.get();
            advance(iterator, ref);
            return k;
          }

          @Override
          public void remove() {
            ConcurrentWeakIdentityHashMap.this.remove(ref.get());
            advance(iterator, ref);
          }
        };
      }

      private void advance(Iterator<WeakReference<K>> iterator, AtomicReference<K> ref) {
        while (iterator.hasNext()) {
          K next = iterator.next().get();
          if (next != null) {
            ref.set(next);
            return;
          }
        }
        ref.set(null);
      }

      @Override
      public boolean contains(Object o) {
        return ConcurrentWeakIdentityHashMap.this.containsKey(o);
      }

      @Override
      public int size() {
        return map.size();
      }
    };
  }

  @Override
  public Collection<V> values() {
    purgeKeys();
    return map.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new AbstractSet<Entry<K, V>>() {
      @Override
      public Iterator<Entry<K, V>> iterator() {
        purgeKeys();
        final Iterator<Entry<WeakReference<K>, V>> iterator = map.entrySet().iterator();
        final AtomicReference<Entry<K, V>> ref = new AtomicReference<Entry<K, V>>();
        advance(iterator, ref);

        return new Iterator<Entry<K, V>>() {
          @Override
          public boolean hasNext() {
            return ref.get() != null;
          }

          @Override
          public Entry<K, V> next() {
            Entry<K, V> entry = ref.get();
            advance(iterator, ref);
            return entry;
          }

          @Override
          public void remove() {
            ConcurrentWeakIdentityHashMap.this.remove(ref.get().getKey());
            advance(iterator, ref);
          }
        };
      }

      private void advance(Iterator<Entry<WeakReference<K>, V>> iterator, AtomicReference<Entry<K, V>> ref) {
        while (iterator.hasNext()) {
          Entry<WeakReference<K>, V> next = iterator.next();
          K key = next.getKey().get();
          if (key != null) {
            ref.set(new AbstractMap.SimpleEntry<K, V>(key, next.getValue()));
            return;
          }
        }
        ref.set(null);
      }

      @Override
      public int size() {
        return map.size();
      }
    };
  }

  private void purgeKeys() {
    Reference<? extends K> reference;
    while ((reference = queue.poll()) != null) {
      map.remove(reference);
    }
  }

  private WeakReference<K> newKey(final K key) {
    return new WeakReference<K>(key, queue);
  }

  private static class WeakReference<T> extends java.lang.ref.WeakReference<T> {

    private final int hashCode;

    private WeakReference(final T referent, final ReferenceQueue<? super T> q) {
      super(referent, q);
      hashCode = referent.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
      return obj != null && obj.getClass() == this.getClass() && (this == obj || this.get() == ((WeakReference)obj).get());
    }

    @Override
    public int hashCode() {
      return hashCode;
    }
  }
}
