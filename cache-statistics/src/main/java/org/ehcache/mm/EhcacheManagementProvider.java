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
import org.ehcache.spi.ServiceProvider;
import org.ehcache.spi.service.ServiceConfiguration;
import org.ehcache.util.ConcurrentWeakIdentityHashMap;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Ludovic Orban
 */
public class EhcacheManagementProvider implements ManagementProvider<Ehcache<?, ?>> {

  private final ConcurrentMap<Ehcache, EhcacheActionManager> actions = new ConcurrentWeakIdentityHashMap<Ehcache, EhcacheActionManager>();

  @Override
  public void start(ServiceConfiguration<?> config, ServiceProvider serviceProvider) {
  }

  @Override
  public void stop() {
    actions.clear();
  }

  @Override
  public void registerActions(Ehcache<?, ?> ehcache) {
    actions.putIfAbsent(ehcache, new EhcacheActionManager(ehcache));
  }

  @Override
  public void unregisterActions(Ehcache ehcache) {
    actions.remove(ehcache);
  }

  @Override
  public Collection<?> actions() {
    return actions.values();
  }
}
