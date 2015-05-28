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
package org.ehcache.mm;

import org.ehcache.Ehcache;

/**
 * @author Ludovic Orban
 */
public class EhcacheActionManager<K, V> {

  private final Ehcache<K, V> ehcache;

  public EhcacheActionManager(Ehcache<K, V> ehcache) {
    this.ehcache = ehcache;
  }

  public void clear() {
    ehcache.clear();
  }

  public V get(K key) {
    return ehcache.get(key);
  }

  Class<V> get_returnType() {
    return ehcache.getRuntimeConfiguration().getValueType();
  }

  String get_parameterName_0() {
    return "key";
  }
  Class<K> get_parameterType_0() {
    return ehcache.getRuntimeConfiguration().getKeyType();
  }

  public void put(K key, V value) {
    ehcache.put(key, value);
  }

  String put_parameterName_0() {
    return "key";
  }
  Class<K> put_parameterType_0() {
    return ehcache.getRuntimeConfiguration().getKeyType();
  }

  String put_parameterName_1() {
    return "value";
  }
  Class<V> put_parameterType_1() {
    return ehcache.getRuntimeConfiguration().getValueType();
  }

}
