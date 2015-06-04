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

import org.ehcache.EhcacheManager;
import org.terracotta.management.capabilities.context.CapabilityContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class CapabilityContextProvider extends AbstractActionProvider<EhcacheManager, EhcacheManagerContext> {

  @Override
  protected EhcacheManagerContext createActionWrapper(EhcacheManager ehcacheManager) {
    return new EhcacheManagerContext(ehcacheManager);
  }

  @Override
  public Class<EhcacheManager> managedType() {
    return EhcacheManager.class;
  }

  @Override
  public CapabilityContext capabilityContext() {
    return new CapabilityContext(Collections.<CapabilityContext.Attribute>emptySet());
  }

  public Collection<String> getCacheManagerNames() {
    Collection<String> allCacheManagerNames = new ArrayList();
    for (Map.Entry<EhcacheManager, EhcacheManagerContext> ehcacheManagerEhcacheManagerContextEntry : actions.entrySet()) {
      allCacheManagerNames.addAll(ehcacheManagerEhcacheManagerContextEntry.getValue().cacheManagerNames());
    }
    return allCacheManagerNames;
  }

  public Collection<String> getCacheNames(String cacheManagerName) {
    Collection<String> allCacheNames = new ArrayList();
    for (Map.Entry<EhcacheManager, EhcacheManagerContext> ehcacheManagerEhcacheManagerContextEntry : actions.entrySet()) {
      allCacheNames.addAll(ehcacheManagerEhcacheManagerContextEntry.getValue().cacheNames(cacheManagerName));
    }
    return allCacheNames;
  }

}
