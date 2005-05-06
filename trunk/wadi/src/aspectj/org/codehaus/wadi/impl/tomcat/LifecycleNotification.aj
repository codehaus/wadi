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

package org.codehaus.wadi.impl.tomcat;

import org.apache.catalina.Lifecycle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TC carries so much notification baggage in its code that it seemed
// to be begging to have it Aspected out...

/**
 * Performs Tomcat proprietary container lifecycle notifications
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public aspect
  LifecycleNotification
{
  protected static final Log _log=LogFactory.getLog(LifecycleNotification.class);

  pointcut start(TomcatManager manager) : execution(void TomcatManager.start()) && target(manager);
  before(TomcatManager manager) : start(manager) {notify(manager, Lifecycle.START_EVENT);}

  pointcut stop(TomcatManager manager) : execution(void TomcatManager.stop()) && target(manager);
  before(TomcatManager manager) : stop(manager) {notify(manager, Lifecycle.STOP_EVENT);}

  public void
    notify(TomcatManager manager, String event)
    {
      _log.trace(event+" notification sent");
      manager._lifecycleListeners.fireLifecycleEvent(event, null);
    }
}
