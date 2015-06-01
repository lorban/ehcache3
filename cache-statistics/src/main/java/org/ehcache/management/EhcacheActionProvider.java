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

import org.ehcache.Ehcache;
import org.ehcache.spi.ServiceProvider;
import org.ehcache.spi.service.ServiceConfiguration;
import org.ehcache.util.ConcurrentWeakIdentityHashMap;
import org.terracotta.management.capabilities.CallCapability;
import org.terracotta.management.capabilities.Capability;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Ludovic Orban
 */
public class EhcacheActionProvider implements ManagementProvider<Ehcache<?, ?>> {

  private final ConcurrentMap<Ehcache, EhcacheActionWrapper> actions = new ConcurrentWeakIdentityHashMap<Ehcache, EhcacheActionWrapper>();
  private volatile ManagementRegistry managementRegistry;

  @Override
  public void start(ServiceConfiguration<?> config, ServiceProvider serviceProvider) {
    managementRegistry = serviceProvider.findService(ManagementRegistry.class);
    managementRegistry.support(this);
  }

  @Override
  public void stop() {
    managementRegistry.unsupport(this);
    actions.clear();
  }

  @Override
  public void register(Ehcache<?, ?> ehcache) {
    actions.putIfAbsent(ehcache, new EhcacheActionWrapper(ehcache));
  }

  @Override
  public void unregister(Ehcache<?, ?> ehcache) {
    actions.remove(ehcache);
  }

  @Override
  public Class<Ehcache<?, ?>> managedType() {
    return (Class) Ehcache.class;
  }

  @Override
  public Set<?> capabilities() {
    return listManagementCapabilities();
  }

  private Set<Capability> listManagementCapabilities() {
    Set<Capability> capabilities = new HashSet<Capability>();

    Collection actions = this.actions.values();
    for (Object action : actions) {
      Class<?> actionClass = action.getClass();
      Method[] methods = actionClass.getMethods();

      for (Method method : methods) {
        Annotation[] declaredAnnotations = method.getDeclaredAnnotations();
        boolean expose = false;
        for (Annotation declaredAnnotation : declaredAnnotations) {
          if (declaredAnnotation.annotationType() == Exposed.class) {
            expose = true;
            break;
          }
        }
        if (!expose) {
          continue;
        }

        String methodName = method.getName();
        Class<?> returnType = method.getReturnType();

        Class<?>[] parameterTypes = method.getParameterTypes();
        List<String> parameterNames = new ArrayList<String>();

        for (int i = 0; i < parameterTypes.length; i++) {
          Annotation[] parameterAnnotations = method.getParameterAnnotations()[i];
          boolean named = false;
          for (Annotation parameterAnnotation : parameterAnnotations) {
            if (parameterAnnotation instanceof Named) {
              Named namedAnnotation = (Named) parameterAnnotation;
              parameterNames.add(namedAnnotation.value());
              named = true;
              break;
            }
          }
          if (!named) {
            parameterNames.add("arg" + i);
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

}
