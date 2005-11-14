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
package org.codehaus.wadi.test;

import org.activecluster.ClusterEvent;
import org.activecluster.ClusterListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class MyClusterListener
    implements ClusterListener
  {
	protected Log _log = LogFactory.getLog(getClass());

	public void
      onNodeAdd(ClusterEvent ce)
    {
        if ( _log.isInfoEnabled() ) {

            _log.info("node added: " + ce.getNode());
        }
    }

    public void
      onNodeFailed(ClusterEvent ce)
    {
        if ( _log.isInfoEnabled() ) {

            _log.info("node failed: " + ce.getNode());
        }
    }

    public void
      onNodeRemoved(ClusterEvent ce)
    {
        if ( _log.isInfoEnabled() ) {

            _log.info("node removed: " + ce.getNode());
        }
    }

    public void
      onNodeUpdate(ClusterEvent ce)
    {
        if ( _log.isInfoEnabled() ) {

            _log.info("node updated: " + ce.getNode());
        }
    }

    public void
      onCoordinatorChanged(ClusterEvent ce)
    {
        if ( _log.isInfoEnabled() ) {

            _log.info("coordinator changed: " + ce.getNode());
        }
    }
  }
