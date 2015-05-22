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
import org.ehcache.config.CacheConfigurationBuilder;
import org.ehcache.config.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.management.stats.CoreStatisticsProvider;
import org.ehcache.management.stats.ExtendedStatisticsProvider;
import org.junit.Test;

/**
 * @author Ludovic Orban
 */
public class StrawMan {


  @Test
  public void testStoreByValue() {
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().using(new ExtendedStatisticsProvider()).build(false);
    cacheManager.init();

    final Cache<Long, String> cache1 = cacheManager.createCache("cache1",
        CacheConfigurationBuilder.newCacheConfigurationBuilder().withResourcePools(ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1, EntryUnit.ENTRIES))
            .buildConfig(Long.class, String.class));


    cacheManager.close();
  }


}
