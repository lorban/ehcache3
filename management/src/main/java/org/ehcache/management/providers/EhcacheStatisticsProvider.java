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
package org.ehcache.management.providers;

import org.ehcache.Ehcache;
import org.ehcache.management.config.StatisticsProviderConfiguration;
import org.ehcache.util.ConcurrentWeakIdentityHashMap;
import org.terracotta.management.capabilities.Capability;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Ludovic Orban
 */
public class EhcacheStatisticsProvider implements ManagementProvider<Ehcache> {

  private final ConcurrentMap<Ehcache, EhcacheStatistics> statistics = new ConcurrentWeakIdentityHashMap<Ehcache, EhcacheStatistics>();

  private final StatisticsProviderConfiguration configuration;
  private final ScheduledExecutorService executor;

  public EhcacheStatisticsProvider(StatisticsProviderConfiguration statisticsProviderConfiguration, ScheduledExecutorService executor) {
    this.configuration = statisticsProviderConfiguration;
    this.executor = executor;
  }

  @Override
  public void register(Ehcache ehcache) {
    statistics.putIfAbsent(ehcache, new EhcacheStatistics(ehcache, configuration, executor));
  }

  @Override
  public void unregister(Ehcache ehcache) {
    EhcacheStatistics removed = statistics.remove(ehcache);
    if (removed != null) {
      removed.dispose();
    }
  }

  @Override
  public Class<Ehcache> managedType() {
    return Ehcache.class;
  }

  @Override
  public Set<Capability> capabilities() {
    Set<Capability> capabilities = new HashSet<Capability>();
    for (EhcacheStatistics ehcacheStatistics : this.statistics.values()) {
      capabilities.addAll(ehcacheStatistics.capabilities());
    }
    return capabilities;
  }

}
