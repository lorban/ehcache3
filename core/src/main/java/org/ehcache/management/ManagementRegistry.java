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

import org.ehcache.spi.service.Service;

import java.util.Collection;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public interface ManagementRegistry extends Service {

  <T> void support(ManagementProvider<T> managementProvider);

  <T> void unsupport(ManagementProvider<T> managementProvider);

  <T> void register(Class<T> managedType, T managedObject);

  <T> void unregister(Class<T> managedType, T managedObject);

  <T> Map<String, Collection<T>> capabilities();

}