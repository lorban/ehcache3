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
import org.ehcache.config.StatisticsProviderConfiguration;
import org.ehcache.spi.ServiceProvider;
import org.ehcache.spi.service.ServiceConfiguration;
import org.ehcache.util.ConcurrentWeakIdentityHashMap;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Ludovic Orban
 */
public class EhcacheStatisticsProvider implements StatisticsProvider<Ehcache> {

  private final ConcurrentMap<Ehcache, EhcacheStatistics> statistics = new ConcurrentWeakIdentityHashMap<Ehcache, EhcacheStatistics>();

  private volatile StatisticsProviderConfiguration configuration;
  private volatile ScheduledExecutorService executor;

  @Override
  public void start(ServiceConfiguration<?> config, ServiceProvider serviceProvider) {
    executor = Executors.newScheduledThreadPool(1);
    configuration = (StatisticsProviderConfiguration) config;
  }

  @Override
  public void stop() {
    executor.shutdownNow();
    executor = null;
    statistics.clear();
    configuration = null;
  }

  @Override
  public void createStatistics(Ehcache contextObject) {
    statistics.putIfAbsent(contextObject, new EhcacheStatistics(contextObject, configuration, executor));
  }

  @Override
  public void deleteStatistics(Ehcache contextObject) {
    EhcacheStatistics removed = statistics.remove(contextObject);
    if (removed != null) {
      removed.dispose();
    }
  }

  @Override
  public Collection<Ehcache> contextObjects() {
    return statistics.keySet();
  }

}
