/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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

package org.codehaus.wadi;


/**
 * Abstracts out the decision of when a session should be evicted to
 * long-term storage.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface
  EvictionPolicy
{
  /**
   * Decide whether a session should be evicted/migrated out of the
   * container to save resources.
   *
   * @param currentTimeMillis a <code>long</code> value
   * @param impl a <code>HttpSessionImpl</code> value
   * @return a <code>boolean</code> value
   */
  boolean evictable(long currentTimeMillis, HttpSessionImpl impl);
}
