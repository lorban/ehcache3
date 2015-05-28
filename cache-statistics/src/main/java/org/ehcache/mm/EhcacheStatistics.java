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

import org.ehcache.config.StatisticsProviderConfiguration;
import org.ehcache.statistics.CacheOperationOutcomes;
import org.terracotta.context.extended.StatisticsRegistry;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Ludovic Orban
 */
public class EhcacheStatistics {

  private static final Set<CacheOperationOutcomes.PutOutcome> ALL_CACHE_PUT_OUTCOMES = EnumSet.allOf(CacheOperationOutcomes.PutOutcome.class);
  private static final Set<CacheOperationOutcomes.GetOutcome> ALL_CACHE_GET_OUTCOMES = EnumSet.allOf(CacheOperationOutcomes.GetOutcome.class);
  private static final Set<CacheOperationOutcomes.GetOutcome> ALL_CACHE_MISS_OUTCOMES = EnumSet.of(CacheOperationOutcomes.GetOutcome.FAILURE, CacheOperationOutcomes.GetOutcome.MISS_NO_LOADER, CacheOperationOutcomes.GetOutcome.MISS_WITH_LOADER);
  private static final Set<CacheOperationOutcomes.RemoveOutcome> ALL_CACHE_REMOVE_OUTCOMES = EnumSet.allOf(CacheOperationOutcomes.RemoveOutcome.class);
  private static final Set<CacheOperationOutcomes.GetOutcome> GET_WITH_LOADER_OUTCOMES = EnumSet.of(CacheOperationOutcomes.GetOutcome.HIT_WITH_LOADER, CacheOperationOutcomes.GetOutcome.MISS_WITH_LOADER);
  private static final Set<CacheOperationOutcomes.GetOutcome> GET_NO_LOADER_OUTCOMES = EnumSet.of(CacheOperationOutcomes.GetOutcome.HIT_NO_LOADER, CacheOperationOutcomes.GetOutcome.MISS_NO_LOADER);
  private static final Set<CacheOperationOutcomes.CacheLoadingOutcome> ALL_CACHE_LOADER_OUTCOMES = EnumSet.allOf(CacheOperationOutcomes.CacheLoadingOutcome.class);

  private final StatisticsRegistry statisticsContainer;

  EhcacheStatistics(Object contextObject, StatisticsProviderConfiguration configuration, ScheduledExecutorService executor) {
    this.statisticsContainer = new StatisticsRegistry(StandardOperationStatistic.class, contextObject, executor, configuration.averageWindowDuration(),
        configuration.averageWindowUnit(), configuration.historySize(), configuration.historyInterval(), configuration.historyIntervalUnit(),
        configuration.timeToDisable(), configuration.timeToDisableUnit());

    statisticsContainer.registerCompoundOperation(StandardOperationStatistic.CACHE_GET, ALL_CACHE_GET_OUTCOMES, Collections.<String, Object>singletonMap("Result", "AllCacheGet"));
    statisticsContainer.registerCompoundOperation(StandardOperationStatistic.CACHE_GET, ALL_CACHE_MISS_OUTCOMES, Collections.<String, Object>singletonMap("Result", "AllCacheMiss"));
    statisticsContainer.registerCompoundOperation(StandardOperationStatistic.CACHE_PUT, ALL_CACHE_PUT_OUTCOMES, Collections.<String, Object>singletonMap("Result", "AllCachePut"));
    statisticsContainer.registerCompoundOperation(StandardOperationStatistic.CACHE_REMOVE, ALL_CACHE_REMOVE_OUTCOMES, Collections.<String, Object>singletonMap("Result", "AllCacheRemove"));
    statisticsContainer.registerCompoundOperation(StandardOperationStatistic.CACHE_GET, GET_WITH_LOADER_OUTCOMES, Collections.<String, Object>singletonMap("Result", "GetWithLoader"));
    statisticsContainer.registerCompoundOperation(StandardOperationStatistic.CACHE_GET, GET_NO_LOADER_OUTCOMES, Collections.<String, Object>singletonMap("Result", "GetNoLoader"));
    statisticsContainer.registerCompoundOperation(StandardOperationStatistic.CACHE_LOADING, ALL_CACHE_LOADER_OUTCOMES, Collections.<String, Object>singletonMap("Result", "AllCacheLoader"));
    statisticsContainer.registerRatio(StandardOperationStatistic.CACHE_GET, EnumSet.of(CacheOperationOutcomes.GetOutcome.HIT_NO_LOADER), ALL_CACHE_GET_OUTCOMES, Collections.<String, Object>singletonMap("Ratio", "Hit"));
  }

  public void dispose() {
    statisticsContainer.clearRegistrations();
  }
}
