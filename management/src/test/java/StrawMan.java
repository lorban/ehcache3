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

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.CacheManagerBuilder;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.CacheConfigurationBuilder;
import org.ehcache.config.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.management.ManagementRegistry;
import org.ehcache.management.registry.DefaultManagementRegistryFactoryConfiguration;
import org.ehcache.management.config.EhcacheStatisticsProviderConfiguration;
import org.ehcache.spi.ServiceProvider;
import org.ehcache.spi.service.Service;
import org.ehcache.spi.service.ServiceConfiguration;
import org.junit.Test;
import org.terracotta.management.capabilities.Capability;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Ludovic Orban
 */
public class StrawMan {

  @Test
  public void test() {
    CacheConfiguration<Long, String> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder()
        .withResourcePools(ResourcePoolsBuilder.newResourcePoolsBuilder().heap(10, EntryUnit.ENTRIES).build())
        .buildConfig(Long.class, String.class);

    CapabilitiesCollectorService capabilitiesCollectorService = new CapabilitiesCollectorService();
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .withCache("aCache", cacheConfiguration)
        .using(capabilitiesCollectorService)
        .using(new DefaultManagementRegistryFactoryConfiguration().addConfiguration(new EhcacheStatisticsProviderConfiguration(5 * 60, TimeUnit.SECONDS, 100, 1, TimeUnit.SECONDS, 30, TimeUnit.SECONDS)))
        .build(false);
    cacheManager.init();

    final Cache<Long, String> cache1 = cacheManager.createCache("cache1",
        CacheConfigurationBuilder.newCacheConfigurationBuilder().withResourcePools(ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1, EntryUnit.ENTRIES))
            .buildConfig(Long.class, String.class));

    Map<String, Collection<Capability>> capabilities = capabilitiesCollectorService.listManagementCapabilities();

    cacheManager.close();
  }

  static class CapabilitiesCollectorService implements Service {

    private volatile ManagementRegistry managementRegistry;

    @Override
    public void start(ServiceConfiguration<?> config, ServiceProvider serviceProvider) {
      managementRegistry = serviceProvider.findService(ManagementRegistry.class);
    }

    @Override
    public void stop() {
      managementRegistry = null;
    }

    public Map<String, Collection<Capability>> listManagementCapabilities() {
      return managementRegistry.capabilities();
    }

  }


}
