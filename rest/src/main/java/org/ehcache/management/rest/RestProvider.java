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
import org.ehcache.mm.ManagementProvider;
import org.ehcache.mm.StatisticsProvider;
import org.terracotta.context.ContextManager;
import org.terracotta.context.TreeNode;
import org.terracotta.management.capabilities.CallCapability;
import org.terracotta.management.capabilities.Capability;
import org.terracotta.management.capabilities.CapabilityCategory;
import org.terracotta.management.capabilities.StatisticCapability;
import org.terracotta.management.stats.StatisticType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
  private volatile ManagementProvider managementProvider;

  @Override
  public void start(ServiceConfiguration<?> config, ServiceProvider serviceProvider) {
    statisticsProvider = serviceProvider.findService(StatisticsProvider.class);
    managementProvider = serviceProvider.findService(ManagementProvider.class);
  }

  @Override
  public void stop() {
    statisticsProvider = null;
    managementProvider = null;
  }

  public Set<Capability> listManagementCapabilities() {
    Set<Capability> capabilities = new HashSet<Capability>();

    Collection actions = managementProvider.actions();
    for (Object action : actions) {
      Class<?> actionClass = action.getClass();
      Method[] methods = actionClass.getMethods();

      for (Method method : methods) {
        if (method.getDeclaringClass() == Object.class) {
          continue;
        }

        String methodName = method.getName();
        Class<?> returnType = null;

        try {
          Method returnTypeMethod = actionClass.getDeclaredMethod(methodName + "_returnType");
          returnTypeMethod.setAccessible(true);
          returnType = (Class<?>) returnTypeMethod.invoke(action);
        } catch (NoSuchMethodException nsme) {
          // ignore
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
        if (returnType == null) {
          returnType = method.getReturnType();
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        List<String> parameterNames = new ArrayList<String>();

        for (int i = 0; i < parameterTypes.length; i++) {
          try {
            Method parameterTypeMethod = actionClass.getDeclaredMethod(methodName + "_parameterType_" + i);
            parameterTypeMethod.setAccessible(true);
            parameterTypes[i] = (Class<?>) parameterTypeMethod.invoke(action);
          } catch (NoSuchMethodException nsme) {
            // ignore
          } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }

          try {
            Method parameterNameMethod = actionClass.getDeclaredMethod(methodName + "_parameterName_" + i);
            parameterNameMethod.setAccessible(true);
            parameterNames.add((String) parameterNameMethod.invoke(action));
          } catch (NoSuchMethodException e) {
            parameterNames.add("arg" + i);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
          }
        }

        List<CallCapability.Parameter> parameters = new ArrayList<CallCapability.Parameter>();
        for (int i=0;i<parameterTypes.length;i++) {
          parameters.add(new CallCapability.Parameter(parameterNames.get(i), parameterTypes[i].getName()));
        }

        capabilities.add(new CallCapability(methodName, returnType.getName(), parameters));
      }
    }

    return capabilities;
  }

  public Set<Capability> listMonitoringCapabilities() {
    Set<Capability> capabilities = new HashSet<Capability>();

    Collection contextObjects = statisticsProvider.contextObjects();
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
