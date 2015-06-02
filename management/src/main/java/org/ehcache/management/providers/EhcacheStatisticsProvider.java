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
import org.terracotta.context.ContextManager;
import org.terracotta.context.TreeNode;
import org.terracotta.management.capabilities.Capability;
import org.terracotta.management.capabilities.CapabilityCategory;
import org.terracotta.management.capabilities.StatisticCapability;
import org.terracotta.management.stats.StatisticType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    return listMonitoringCapabilities();
  }


  private Set<Capability> listMonitoringCapabilities() {
    Set<Capability> capabilities = new HashSet<Capability>();

    Collection contextObjects = this.statistics.keySet();
    for (Object contextObject : contextObjects) {
      TreeNode treeNode = ContextManager.nodeFor(contextObject);
      Set<Capability> nodeCapabilities = buildMonitoringCapabilities(treeNode);
      capabilities.addAll(nodeCapabilities);
    }

    return capabilities;
  }

  private Set<Capability> buildMonitoringCapabilities(TreeNode treeNode) {
    Set<Capability> capabilities = new HashSet<Capability>();

    Object attributesProperty = treeNode.getContext().attributes().get("properties");
    if (attributesProperty != null && attributesProperty instanceof Map) {
      Map<String, Object> attributes = (Map<String, Object>) attributesProperty;

      Object setting = attributes.get("Setting");
      if (setting != null) {
        capabilities.add(new StatisticCapability(setting.toString(), StatisticType.SETTING));
      }

      Object resultObject = attributes.get("Result");
      if (resultObject != null) {
        String resultName = resultObject.toString();

        List<Capability> statistics = new ArrayList<Capability>();
        statistics.add(new StatisticCapability(resultName + "Count", StatisticType.SAMPLED_COUNTER));
        statistics.add(new StatisticCapability(resultName + "Rate", StatisticType.SAMPLED_RATE));
        statistics.add(new StatisticCapability(resultName + "LatencyMinimum", StatisticType.SAMPLED_DURATION));
        statistics.add(new StatisticCapability(resultName + "LatencyMaximum", StatisticType.SAMPLED_DURATION));
        statistics.add(new StatisticCapability(resultName + "LatencyAverage", StatisticType.SAMPLED_RATIO));

        capabilities.add(new CapabilityCategory(resultName, statistics));
      }

      Object ratioObject = attributes.get("Ratio");
      if (ratioObject != null) {
        capabilities.add(new StatisticCapability(ratioObject.toString() + "Ratio", StatisticType.RATIO));
      }

    }

    Set<? extends TreeNode> children = treeNode.getChildren();
    for (TreeNode child : children) {
      Set<Capability> childCapabilities = buildMonitoringCapabilities(child);
      capabilities.addAll(childCapabilities);
    }

    return capabilities;
  }

}
