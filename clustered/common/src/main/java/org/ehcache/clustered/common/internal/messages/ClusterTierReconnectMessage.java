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

package org.ehcache.clustered.common.internal.messages;

import java.util.HashSet;
import java.util.Set;

public class ClusterTierReconnectMessage {

  private final Set<Long> hashInvalidationsInProgress = new HashSet<>();
  private boolean clearInProgress = false;

  public void addInvalidationsInProgress(Set<Long> hashInvalidationsInProgress) {
    this.hashInvalidationsInProgress.addAll(hashInvalidationsInProgress);
  }

  public Set<Long> getInvalidationsInProgress() {
    return hashInvalidationsInProgress;
  }

  public void clearInProgress() {
    clearInProgress = true;
  }

  public boolean isClearInProgress() {
    return clearInProgress;
  }

}
