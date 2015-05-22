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
package org.ehcache.management.stats;

import org.ehcache.Ehcache;
import org.ehcache.spi.ServiceProvider;
import org.ehcache.spi.service.ServiceConfiguration;
import org.ehcache.statistics.CacheOperationOutcomes;
import org.ehcache.statistics.StatisticsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.context.ContextManager;
import org.terracotta.context.TreeNode;
import org.terracotta.context.query.Matcher;
import org.terracotta.context.query.Matchers;
import org.terracotta.context.query.Query;
import org.terracotta.statistics.OperationStatistic;
import org.terracotta.statistics.extended.CompoundOperation;
import org.terracotta.statistics.extended.CompoundOperationImpl;
import org.terracotta.statistics.extended.NullCompoundOperation;
import org.terracotta.statistics.extended.Result;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.terracotta.context.query.Matchers.attributes;
import static org.terracotta.context.query.Matchers.context;
import static org.terracotta.context.query.Matchers.hasAttribute;
import static org.terracotta.context.query.Matchers.identifier;
import static org.terracotta.context.query.Matchers.subclassOf;
import static org.terracotta.context.query.QueryBuilder.queryBuilder;

/**
 * @author Ludovic Orban
 */
public class ExtendedStatisticsProvider implements StatisticsProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedStatisticsProvider.class);

  private final ConcurrentHashMap<Object, StatisticsHolder> statistics = new ConcurrentHashMap<Object, StatisticsHolder>(16, .8f, 1);


  private static final TimeUnit AVERAGE_PERIOD_UNIT = SECONDS;
  private static final TimeUnit HISTORY_PERIOD_UNIT = SECONDS;

  private volatile ScheduledExecutorService executor;
  private volatile long averageWindowSize = 5L * 60L;
  private volatile int historySize = 100;
  private volatile long historyInterval = 1;


  @Override
  public void start(ServiceConfiguration<?> config, ServiceProvider serviceProvider) {
    executor = Executors.newScheduledThreadPool(1);
  }

  @Override
  public void stop() {
    statistics.clear();
    executor.shutdownNow();
    executor = null;
  }

  @Override
  public void createStatistics(Object contextObject) {
    if (contextObject instanceof Ehcache) {
      LOGGER.info("creating stats for " + contextObject);
      statistics.putIfAbsent(contextObject, new StatisticsHolder(contextObject));
    }
  }

  public void deleteStatistics(Object contextObject) {
    if (contextObject instanceof Ehcache) {
      LOGGER.info("destroying stats for " + contextObject);
      statistics.remove(contextObject);
    }
  }


  static final Set<CacheOperationOutcomes.PutOutcome> ALL_CACHE_PUT_OUTCOMES = EnumSet.allOf(CacheOperationOutcomes.PutOutcome.class);
  static final Set<CacheOperationOutcomes.GetOutcome> ALL_CACHE_GET_OUTCOMES = EnumSet.allOf(CacheOperationOutcomes.GetOutcome.class);
  static final Set<CacheOperationOutcomes.GetOutcome> ALL_CACHE_MISS_OUTCOMES = EnumSet.of(CacheOperationOutcomes.GetOutcome.FAILURE, CacheOperationOutcomes.GetOutcome.MISS_NO_LOADER, CacheOperationOutcomes.GetOutcome.MISS_WITH_LOADER);
  static final Set<CacheOperationOutcomes.RemoveOutcome> ALL_CACHE_REMOVE_OUTCOMES = EnumSet.allOf(CacheOperationOutcomes.RemoveOutcome.class);
  static final Set<CacheOperationOutcomes.GetOutcome> GET_WITH_LOADER_OUTCOMES = EnumSet.of(CacheOperationOutcomes.GetOutcome.HIT_WITH_LOADER, CacheOperationOutcomes.GetOutcome.MISS_WITH_LOADER);
  static final Set<CacheOperationOutcomes.GetOutcome> GET_NO_LOADER_OUTCOMES = EnumSet.of(CacheOperationOutcomes.GetOutcome.HIT_NO_LOADER, CacheOperationOutcomes.GetOutcome.MISS_NO_LOADER);
  static final Set<CacheOperationOutcomes.CacheLoadingOutcome> ALL_CACHE_LOADER_OUTCOMES = EnumSet.allOf(CacheOperationOutcomes.CacheLoadingOutcome.class);

  final class StatisticsHolder {

    private final Result allCacheGet;
    private final Result allCacheMiss;
    private final Result allCachePut;
    private final Result allCacheRemove;
    private final Result getWithLoading;
    private final Result getNoLoading;
    private final Result cacheLoading;

    private final ConcurrentMap<StandardOperationStatistic, CompoundOperation<?>> standardOperations = new ConcurrentHashMap<StandardOperationStatistic, CompoundOperation<?>>();
    private final Object contextObject;

    StatisticsHolder(Object contextObject) {
      this.contextObject = contextObject;

      discoverStandardOperationStatistics();

      this.allCacheGet = get().compound(ALL_CACHE_GET_OUTCOMES);
      this.allCacheMiss = get().compound(ALL_CACHE_MISS_OUTCOMES);
      this.allCachePut = put().compound(ALL_CACHE_PUT_OUTCOMES);
      this.allCacheRemove = remove().compound(ALL_CACHE_REMOVE_OUTCOMES);
      this.getWithLoading = get().compound(GET_WITH_LOADER_OUTCOMES);
      this.getNoLoading = get().compound(GET_NO_LOADER_OUTCOMES);
      this.cacheLoading = ((CompoundOperation<CacheOperationOutcomes.CacheLoadingOutcome>) getStandardOperation(StandardOperationStatistic.CACHE_LOADING))
          .compound(ALL_CACHE_LOADER_OUTCOMES);
    }

    CompoundOperation<CacheOperationOutcomes.GetOutcome> get() {
      return (CompoundOperation<CacheOperationOutcomes.GetOutcome>) getStandardOperation(StandardOperationStatistic.CACHE_GET);
    }

    CompoundOperation<CacheOperationOutcomes.PutOutcome> put() {
      return (CompoundOperation<CacheOperationOutcomes.PutOutcome>) getStandardOperation(StandardOperationStatistic.CACHE_PUT);
    }

    CompoundOperation<CacheOperationOutcomes.RemoveOutcome> remove() {
      return (CompoundOperation<CacheOperationOutcomes.RemoveOutcome>) getStandardOperation(StandardOperationStatistic.CACHE_REMOVE);
    }

    Result allGet() {
      return allCacheGet;
    }

    Result allMiss() {
      return allCacheMiss;
    }

    Result allPut() {
      return allCachePut;
    }

    Result allRemove() {
      return allCacheRemove;
    }

    Result getWithLoading() {
      return getWithLoading;
    }

    Result getNoLoading() {
      return getNoLoading;
    }

    Result cacheLoading() {
      return cacheLoading;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void discoverStandardOperationStatistics() {
      for (final StandardOperationStatistic t : StandardOperationStatistic.values()) {
        OperationStatistic statistic = findOperationStatistic(t);
        if (statistic == null) {
          if (t.required()) {
            throw new IllegalStateException("Required statistic " + t + " not found");
          } else {
            standardOperations.put(t, NullCompoundOperation.instance(t.type()));
          }
        } else {
          standardOperations.put(t, new CompoundOperationImpl(statistic, t.type(),
              averageWindowSize, AVERAGE_PERIOD_UNIT, executor, historySize,
              historyInterval, HISTORY_PERIOD_UNIT));
        }
      }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CompoundOperation<?> getStandardOperation(StandardOperationStatistic statistic) {
      CompoundOperation<?> operation = standardOperations.get(statistic);
      if (operation instanceof NullCompoundOperation<?>) {
        OperationStatistic<?> discovered = findOperationStatistic(statistic);
        if (discovered == null) {
          return operation;
        } else {
          CompoundOperation<?> newOperation = new CompoundOperationImpl(discovered, statistic.type(),
              averageWindowSize, SECONDS, executor, historySize, historyInterval, SECONDS);
          if (standardOperations.replace(statistic, operation, newOperation)) {
            return newOperation;
          } else {
            return standardOperations.get(statistic);
          }
        }
      } else {
        return operation;
      }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private OperationStatistic findOperationStatistic(StandardOperationStatistic statistic) {
      Set<OperationStatistic<? extends Enum>> results = findOperationStatistic(
          statistic.context(), statistic.type(), statistic.operationName(), statistic.tags());
      switch (results.size()) {
        case 0:
          return null;
        case 1:
          return results.iterator().next();
        default:
          throw new IllegalStateException("Duplicate statistics found for " + statistic);
      }
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> Set<OperationStatistic<T>> findOperationStatistic(Query contextQuery, Class<T> type, String name,
                                                                                  final Set<String> tags) {

      Query q = queryBuilder().chain(contextQuery)
          .children().filter(context(identifier(subclassOf(OperationStatistic.class)))).build();


      Set<TreeNode> operationStatisticNodes = q.execute(Collections.singleton(ContextManager.nodeFor(contextObject)));
      Set<TreeNode> result = queryBuilder()
          .filter(
              context(attributes(Matchers.<Map<String, Object>>allOf(hasAttribute("type", type),
                  hasAttribute("name", name), hasAttribute("tags", new Matcher<Set<String>>() {
                    @Override
                    protected boolean matchesSafely(Set<String> object) {
                      return object.containsAll(tags);
                    }
                  }))))).build().execute(operationStatisticNodes);

      if (result.isEmpty()) {
        return Collections.emptySet();
      } else {
        Set<OperationStatistic<T>> statistics = new HashSet<OperationStatistic<T>>();
        for (TreeNode node : result) {
          statistics.add((OperationStatistic<T>) node.getContext().attributes().get("this"));
        }
        return statistics;
      }
    }
  }

}
