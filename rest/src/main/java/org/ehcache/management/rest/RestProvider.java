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
package org.ehcache.management.rest;

import org.ehcache.spi.ServiceProvider;
import org.ehcache.spi.service.Service;
import org.ehcache.spi.service.ServiceConfiguration;
import org.ehcache.statistics.StatisticsProvider;
import org.terracotta.context.ContextManager;
import org.terracotta.context.TreeNode;
import org.terracotta.management.capabilities.Capability;
import org.terracotta.management.capabilities.CapabilityCategory;
import org.terracotta.management.capabilities.SimpleCapability;
import org.terracotta.management.stats.StatisticType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class RestProvider implements Service {

  private volatile StatisticsProvider statisticsProvider;

  @Override
  public void start(ServiceConfiguration<?> config, ServiceProvider serviceProvider) {
    statisticsProvider = serviceProvider.findService(StatisticsProvider.class);
  }

  @Override
  public void stop() {
    statisticsProvider = null;
  }

  public Set<Capability> listCapabilities() {
    Set<Capability> capabilities = new HashSet<Capability>();

    Collection contextObjects = statisticsProvider.contextObjects();
    for (Object contextObject : contextObjects) {
      TreeNode treeNode = ContextManager.nodeFor(contextObject);
      Set<Capability> nodeCapabilities = buildCapabilities(treeNode);
      capabilities.addAll(nodeCapabilities);
    }

    return capabilities;
  }

  private Set<Capability> buildCapabilities(TreeNode treeNode) {
    Set<Capability> capabilities = new HashSet<Capability>();

    Object attributesProperty = treeNode.getContext().attributes().get("properties");
    if (attributesProperty != null && attributesProperty instanceof Map) {
      Map<String,Object> attributes = (Map<String, Object>) attributesProperty;

      Object setting = attributes.get("Setting");
      if (setting != null) {
        capabilities.add(new SimpleCapability(setting.toString(), StatisticType.SETTING));
      }

      Object resultObject = attributes.get("Result");
      if (resultObject != null) {
        String resultName = resultObject.toString();

        List<Capability> statistics = new ArrayList<Capability>();
        statistics.add(new SimpleCapability(resultName + "Count", StatisticType.SAMPLED_COUNTER));
        statistics.add(new SimpleCapability(resultName + "Rate", StatisticType.SAMPLED_RATE));
        statistics.add(new SimpleCapability(resultName + "LatencyMinimum", StatisticType.SAMPLED_DURATION));
        statistics.add(new SimpleCapability(resultName + "LatencyMaximum", StatisticType.SAMPLED_DURATION));
        statistics.add(new SimpleCapability(resultName + "LatencyAverage", StatisticType.SAMPLED_RATIO));

        capabilities.add(new CapabilityCategory(resultName, statistics));
      }

      Object ratioObject = attributes.get("Ratio");
      if (ratioObject != null) {
        capabilities.add(new SimpleCapability(ratioObject.toString() + "Ratio", StatisticType.RATIO));
      }

    }

    Set<? extends TreeNode> children = treeNode.getChildren();
    for (TreeNode child : children) {
      Set<Capability> childCapabilities = buildCapabilities(child);
      capabilities.addAll(childCapabilities);
    }

    return capabilities;
  }

}
