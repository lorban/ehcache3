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
package org.ehcache.management;

import org.ehcache.Ehcache;

/**
 * @author Ludovic Orban
 */
public class EhcacheActionWrapper<K, V> {

  private final Ehcache<K, V> ehcache;

  public EhcacheActionWrapper(Ehcache<K, V> ehcache) {
    this.ehcache = ehcache;
  }

  @Exposed
  public void clear() {
    ehcache.clear();
  }

  @Exposed
  public V get(@Named("key") K key) {
    return ehcache.get(key);
  }

  @Exposed
  public void put(@Named("key") K key, @Named("value") V value) {
    ehcache.put(key, value);
  }

}
