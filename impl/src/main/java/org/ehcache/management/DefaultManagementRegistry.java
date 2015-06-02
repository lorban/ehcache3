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

import org.ehcache.spi.ServiceProvider;
import org.ehcache.spi.service.ServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


  @Override
  public <T> void support(ManagementProvider<T> managementProvider) {
    Lock lock = this.lock.writeLock();
    lock.lock();
    try {
      List<ManagementProvider<?>> managementProviders = this.managementProviders.get(managementProvider.managedType());
      if (managementProviders == null) {
        managementProviders = new ArrayList<ManagementProvider<?>>();
        this.managementProviders.put(managementProvider.managedType(), managementProviders);
      }
      managementProviders.add(managementProvider);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public <T> void unsupport(ManagementProvider<T> managementProvider) {
    Lock lock = this.lock.writeLock();
    lock.lock();
    try {
      List<ManagementProvider<?>> managementProviders = this.managementProviders.get(managementProvider.managedType());
      if (managementProviders != null) {
        managementProviders.remove(managementProvider);
        if (managementProviders.isEmpty()) {
          this.managementProviders.remove(managementProvider.managedType());
        }
      }
    } finally {
      lock.unlock();
    }
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
  public <T> Map<String, Collection<T>> capabilities() {
    Lock lock = this.lock.readLock();
    lock.lock();
    try {
      Map<String, Collection<T>> result = new HashMap<String, Collection<T>>();

      for (List<ManagementProvider<?>> managementProviders : this.managementProviders.values()) {
        for (ManagementProvider<?> managementProvider : managementProviders) {
          ManagementProvider<T> typedManagementProvider = (ManagementProvider<T>) managementProvider;
          Set<T> capa = (Set<T>) typedManagementProvider.capabilities();
          result.put(typedManagementProvider.getClass().getName(), capa);
        }
      }

      return result;
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
