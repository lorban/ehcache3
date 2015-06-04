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
package org.ehcache.management.registry;

import org.ehcache.management.config.EhcacheStatisticsProviderConfiguration;
import org.ehcache.management.config.StatisticsProviderConfiguration;
import org.ehcache.management.providers.ContextProvider;
import org.ehcache.management.providers.EhcacheActionProvider;
import org.ehcache.management.providers.EhcacheManagerActionProvider;
import org.ehcache.management.providers.EhcacheStatisticsProvider;
import org.ehcache.spi.ServiceLocator;
import org.ehcache.spi.service.ServiceConfiguration;
import org.ehcache.spi.service.ServiceFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Ludovic Orban
 */
public class DefaultManagementRegistryFactory implements ServiceFactory<DefaultManagementRegistry> {

  public static final EhcacheStatisticsProviderConfiguration DEFAULT_EHCACHE_STATISTICS_PROVIDER_CONFIGURATION = new EhcacheStatisticsProviderConfiguration(5 * 60, TimeUnit.SECONDS, 100, 1, TimeUnit.SECONDS, 30, TimeUnit.SECONDS);

  @Override
  public DefaultManagementRegistry create(ServiceConfiguration<DefaultManagementRegistry> sc, ServiceLocator serviceLocator) {
    DefaultManagementRegistryFactoryConfiguration serviceConfiguration = (DefaultManagementRegistryFactoryConfiguration) sc;

    EhcacheActionProvider ehcacheActionProvider = new EhcacheActionProvider();
    EhcacheManagerActionProvider ehcacheManagerActionProvider = new EhcacheManagerActionProvider();

    StatisticsProviderConfiguration statisticsProviderConfiguration = null;
    if (serviceConfiguration != null) {
      statisticsProviderConfiguration = serviceConfiguration.getConfigurationFor(EhcacheStatisticsProvider.class);
    }
    if (statisticsProviderConfiguration == null) {
      statisticsProviderConfiguration = DEFAULT_EHCACHE_STATISTICS_PROVIDER_CONFIGURATION;
    }

    //TODO: get this from appropriate service (see #350)
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    EhcacheStatisticsProvider ehcacheStatisticsProvider = new EhcacheStatisticsProvider(statisticsProviderConfiguration, executor);

    ContextProvider contextProvider = new ContextProvider();

    return new DefaultManagementRegistry(ehcacheActionProvider, ehcacheManagerActionProvider, ehcacheStatisticsProvider, contextProvider);
  }

  @Override
  public Class<DefaultManagementRegistry> getServiceType() {
    return DefaultManagementRegistry.class;
  }

}
