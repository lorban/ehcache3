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
import org.ehcache.management.annotations.Exposed;
import org.ehcache.management.annotations.Named;
import org.terracotta.context.TreeNode;
import org.terracotta.context.query.Matcher;
import org.terracotta.context.query.Query;
import org.terracotta.context.query.QueryBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.terracotta.context.query.Matchers.attributes;
import static org.terracotta.context.query.Matchers.context;
import static org.terracotta.context.query.Matchers.hasAttribute;

/**
 * @author Ludovic Orban
 */
public class EhcacheManagerContext {
  private final EhcacheManager ehcacheManager;

  public EhcacheManagerContext(EhcacheManager ehcacheManager) {
    this.ehcacheManager = ehcacheManager;
  }

  @Exposed
  public Collection<String> cacheManagerNames() {
    return Collections.singleton("the-one-and-only");
  }

  @Exposed
  public Collection<String> cacheNames(@Named("cacheManagerName") String cacheManagerName) {
    Query q = QueryBuilder.queryBuilder().descendants()
        .filter(context(attributes(hasAttribute("CacheName", new Matcher<String>() {
          @Override
          protected boolean matchesSafely(String object) {
            return true;
          }
        }))))
        .filter(context(attributes(hasAttribute("tags", new Matcher<Set<String>>() {
          @Override
          protected boolean matchesSafely(Set<String> object) {
            return object.containsAll(Arrays.asList("cache", "exposed"));
          }
        }))))
        .build();

    Set<TreeNode> queryResult = ehcacheManager.getStatisticsManager().query(q);

    Collection<String> result = new ArrayList<String>();
    for (TreeNode treeNode : queryResult) {
      String cacheName = (String) treeNode.getContext().attributes().get("CacheName");
      result.add(cacheName);
    }
    return result;
  }

}
