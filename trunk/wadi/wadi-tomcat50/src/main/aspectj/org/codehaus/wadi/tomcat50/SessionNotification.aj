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

package org.codehaus.wadi.tomcat50;

import java.util.List;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.SessionWrapper;

// corresponds to calls to fireSessionEvent in
// org.apache.catalina.session.StandardSession

// why is the session rather than the manager sending out
// notifications of its construction and destruction? - How am I meant
// to register interest in its construction before it is constructued
// :-)

// why, with all the existing schemes for notification, does TC need
// yet another one?

/**
 * Performs Tomcat internal session lifecycle notifications
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public privileged aspect
  SessionNotification
{
  protected static final Log _log=LogFactory.getLog(SessionNotification.class);

  pointcut notifySessionCreated(TomcatManager manager, HttpSessionListener listener, HttpSessionEvent event)
    : execution(void TomcatManager.notifySessionCreated(HttpSessionListener, HttpSessionEvent)) && args(listener, event) && target(manager);

  pointcut notifySessionDestroyed(TomcatManager manager, HttpSessionListener listener, HttpSessionEvent event)
    : execution(void TomcatManager.notifySessionDestroyed(HttpSessionListener, HttpSessionEvent)) && args(listener, event) && target(manager);

  before(TomcatManager manager, HttpSessionListener listener, HttpSessionEvent event)
    : notifySessionCreated(manager, listener, event)
  {
    _log.trace("notifySessionCreated pointcut");
    notify(event.getSession(), Session.SESSION_CREATED_EVENT);
  }

  after(TomcatManager manager, HttpSessionListener listener, HttpSessionEvent event)
    : notifySessionDestroyed(manager, listener, event)
  {
    _log.trace("notifySessionDestroyed pointcut");
    notify(event.getSession(), Session.SESSION_DESTROYED_EVENT);
  }

  void
    notify(javax.servlet.http.HttpSession session, String name)
  {
    TomcatSession impl=(TomcatSession)((SessionWrapper)session)._session; // TODO - hacky
    List listeners=impl.getSessionListeners();
    int n=listeners.size();
    if (n>0)
    {
      SessionEvent event=new SessionEvent(impl, name, null);
      // tomcat makes a copy and performs the notification outside
      // the synchronized block - does anyone really register with
      // an individual session ?

      // we should consider using a RW lock and taking the R-lock
      // here...
      synchronized (listeners)
      {
	for (int i = 0; i<n; i++)
	  ((SessionListener) listeners.get(i)).sessionEvent(event);
      }
      event=null;
    }
  }
}
