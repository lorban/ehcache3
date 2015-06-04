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

import org.ehcache.management.annotations.Exposed;
import org.ehcache.management.annotations.Named;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Ludovic Orban
 */
public class EventActionsWrapper {

  private final BlockingQueue<Object> events = new LinkedBlockingDeque<Object>();

  @Exposed
  public Collection<Object> consumeEvents(@Named("max") int max) {
    Collection<Object> result = new ArrayList<Object>(max);
    events.drainTo(result, max);
    return result;
  }

  public void queueEvent(Object event) {
    events.add(event);
  }
}
