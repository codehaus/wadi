/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.codehaus.wadi.shared;

// TODO - The API should probably be in terms of multiple sessions
// instead of single ones ?

import java.util.Collection;

public interface
    PassivationStrategy
{
  /**
   * Move a session from internal to external storage, making
   * relevant notifications with correct ClassLoader.
   *
   * @param s a <code>Session</code> value
   */
  boolean passivate(HttpSessionImpl impl);

  /**
   * Move a session from external to internal storage, making
   * relevant notifications with correct ClassLoader.
   *
   * @param id a <code>String</code> value
   * @return a <code>Session</code> value
   */
  boolean activate(String id, HttpSessionImpl impl);

  /**
   * This is called occasionally to collect passivated sessions that
   * have timed out. These need to be loaded back into local memory
   * to be invalidated with all the correct notifications.
   *
   * @return a <code>Collection</code> value
   */
  Collection findTimedOut(long currentTimeMillis, Collection collection);

  /**
   * Are we currently the elected Distributed Garbage Collector?
   *
   * @return a <code>boolean</code> value
   */
  boolean isElected();

  /**
   * Volunteer for the role of Distributed Garbage Collector.
   *
   * @return a <code>boolean</code> value
   */
  boolean standUp();

  /**
   * Resign from the role of Distributed Garbage Collector.
   */
  void standDown();
}
