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

import org.ehcache.management.ManagementRegistry;
import org.ehcache.management.config.StatisticsProviderConfiguration;
import org.ehcache.management.providers.EhcacheStatisticsProvider;
import org.ehcache.management.providers.EventProvider;
import org.ehcache.management.providers.ManagementProvider;
import org.ehcache.spi.ServiceProvider;
import org.ehcache.spi.service.ServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.capabilities.ActionsCapability;
import org.terracotta.management.capabilities.Capability;
import org.terracotta.management.capabilities.StatisticsCapability;
import org.terracotta.management.capabilities.context.Context;
import org.terracotta.management.capabilities.descriptors.Descriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Ludovic Orban
 */
public class DefaultManagementRegistry implements ManagementRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultManagementRegistry.class);

  private final Map<Class<?>, List<ManagementProvider<?>>> managementProviders = new HashMap<Class<?>, List<ManagementProvider<?>>>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final EventProvider eventProvider = new EventProvider();

  DefaultManagementRegistry(ManagementProvider<?>... managementProviders) {
    for (ManagementProvider<?> managementProvider : managementProviders) {
      addSupportFor(managementProvider);
    }
    addSupportFor(eventProvider);
  }

  @Override
  public void sendEvent(Object event) {
    eventProvider.sendEvent(event);
  }

  void addSupportFor(ManagementProvider<?> managementProvider) {
    List<ManagementProvider<?>> managementProviders = this.managementProviders.get(managementProvider.managedType());
    if (managementProviders == null) {
      managementProviders = new ArrayList<ManagementProvider<?>>();
      this.managementProviders.put(managementProvider.managedType(), managementProviders);
    }
    managementProviders.add(managementProvider);
  }

  @Override
  public <T> void register(Class<T> managedType, T managedObject) {
    Lock lock = this.lock.writeLock();
    lock.lock();
    try {
      List<ManagementProvider<?>> managementProviders = this.managementProviders.get(managedType);
      if (managementProviders == null) {
        LOGGER.warn("No registered management provider that supports {}", managedType);
        return;
      }
      for (ManagementProvider<?> managementProvider : managementProviders) {
        ManagementProvider<T> typedManagementProvider = (ManagementProvider<T>) managementProvider;
        typedManagementProvider.register(managedObject);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public <T> void unregister(Class<T> managedType, T managedObject) {
    Lock lock = this.lock.writeLock();
    lock.lock();
    try {
      List<ManagementProvider<?>> managementProviders = this.managementProviders.get(managedType);
      if (managementProviders == null) {
        LOGGER.warn("No registered management provider that supports {}", managedType);
        return;
      }
      for (ManagementProvider<?> managementProvider : managementProviders) {
        ManagementProvider<T> typedManagementProvider = (ManagementProvider<T>) managementProvider;
        typedManagementProvider.unregister(managedObject);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public <T> Collection<T> capabilities() {
    Lock lock = this.lock.readLock();
    lock.lock();
    try {
      Collection<Capability> result = new ArrayList<Capability>();


      for (Map.Entry<Class<?>, List<ManagementProvider<?>>> entry : managementProviders.entrySet()) {
        List<ManagementProvider<?>> managementProviders = entry.getValue();
        for (ManagementProvider<?> managementProvider : managementProviders) {
          if (managementProvider instanceof EhcacheStatisticsProvider) {
            Set<Descriptor> descriptors = managementProvider.descriptions();
            String name = managementProvider.getClass().getName();
            Context context = managementProvider.context();
            EhcacheStatisticsProvider ehcacheStatisticsProvider = (EhcacheStatisticsProvider) managementProvider;
            StatisticsProviderConfiguration configuration = ehcacheStatisticsProvider.getConfiguration();
            StatisticsCapability.Properties properties = new StatisticsCapability.Properties(configuration.averageWindowDuration(),
                configuration.averageWindowUnit(), configuration.historySize(), configuration.historyInterval(),
                configuration.historyIntervalUnit(), configuration.timeToDisable(), configuration.timeToDisableUnit());

            StatisticsCapability statisticsCapability = new StatisticsCapability(name, properties, descriptors, context);
            result.add(statisticsCapability);
          } else {
            Set<Descriptor> descriptors = managementProvider.descriptions();
            String name = managementProvider.getClass().getName();
            Context context = managementProvider.context();

            ActionsCapability actionsCapability = new ActionsCapability(name, descriptors, context);
            result.add(actionsCapability);
          }
        }
      }

      return (Collection<T>) result;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void start(ServiceConfiguration<?> config, ServiceProvider serviceProvider) {
  }

  @Override
  public void stop() {
    Lock lock = this.lock.writeLock();
    lock.lock();
    try {
      managementProviders.clear();
    } finally {
      lock.unlock();
    }
  }
}
