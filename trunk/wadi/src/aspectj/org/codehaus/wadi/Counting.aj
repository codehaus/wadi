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

package org.codehaus.wadi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// abstract out support for all the different counters so they don't
// clutter up the code....

/**
 * Maintains session lifecycle counters
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1.3 $
 */
public aspect
  Counting
{
  private static final Log _log=LogFactory.getLog(Replicating.class);

  pointcut creation(Manager manager) :
  execution(void Manager.notifySessionCreated(String, javax.servlet.http.HttpSession)) && target(manager);
  after(Manager manager) : creation(manager) {manager._sessionCreationCounter++;}

  pointcut destruction(Manager manager) :
  execution(void Manager.notifySessionDestroyed(String, javax.servlet.http.HttpSession)) && target(manager);
  after(Manager manager) : destruction(manager) {manager._sessionDestructionCounter++;}

  // TODO-very ropey - do it better - invalidation should call through the Manager
  pointcut invalidation(HttpSession session, boolean b) :
  execution(void HttpSession.setInvalidated(boolean)) && target(session) && args(b);
  after(HttpSession session, boolean b) : invalidation(session, b) {if (b) session._impl.getWadiManager()._sessionInvalidationCounter++;}

  //  pointcut expiration(HttpSession session, boolean b) :
  //  execution(void HttpSession.setInvalidated(boolean)) && target(session) && args(b);
  //  after(HttpSession session, boolean b) : expiration(session, b) {if (b) session._impl.getWadiManager()._sessionExpirationCounter++;}
}
