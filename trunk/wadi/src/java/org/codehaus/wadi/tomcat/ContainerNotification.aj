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

package org.codehaus.wadi.tomcat;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.catalina.core.StandardContext;


// TODO - if I knew more about AspectJ I'm sure that this could be
// tighter - revisit when I do.

// corresponds to calls to fireContainerEvent in
// org.apache.catalina.session.StandardSession

/**
 * Performs Tomcat container-level notifications of session lifecycle events
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public aspect
  ContainerNotification
{
  protected static final Log _log=LogFactory.getLog(ContainerNotification.class);

  static class
    JumpEntry
  {
    String _before;
    String getBefore(){return _before;}

    String _after;
    String getAfter(){return _after;}

    JumpEntry(String before, String after)
    {
      _before=before;
      _after=after;
    }
  }

  static Map _jumpTable=new HashMap();
  static
  {
    _jumpTable.put("notifySessionCreated"           , new JumpEntry("beforeSessionCreated"           , "afterSessionCreated"));
    _jumpTable.put("notifySessionDestroyed"         , new JumpEntry("beforeSessionDestroyed"         , "afterSessionDestroyed"));
    _jumpTable.put("notifySessionAttributeAdded"    , new JumpEntry("beforeSessionAttributeAdded"    , "afterSessionAttributeAdded"));
    _jumpTable.put("notifySessionAttributeRemoved"  , new JumpEntry("beforeSessionAttributeRemoved"  , "afterSessionAttributeRemoved"));
    _jumpTable.put("notifySessionAttributeReplaced" , new JumpEntry("beforeSessionAttributeReplaced" , "afterSessionAttributeReplaced"));
  }

  pointcut notifySession(Manager manager, HttpSessionListener listener, HttpSessionEvent event)
    : (execution(void Manager.notifySessionCreated(HttpSessionListener, HttpSessionEvent)) ||
       execution(void Manager.notifySessionDestroyed(HttpSessionListener, HttpSessionEvent))) &&
    args(listener, event) &&
    target(manager);

  void
    around(Manager manager, HttpSessionListener listener, HttpSessionEvent event)
    : notifySession(manager, listener, event)
  {
    JumpEntry je=(JumpEntry)_jumpTable.get(thisJoinPointStaticPart.getSignature().getName());

    _log.trace("notifySession pointcut");

    if (manager._container instanceof StandardContext)
    {
      StandardContext ctx=(StandardContext)manager._container;
      try
      {
	ctx.fireContainerEvent(je.getBefore(), listener);
	proceed(manager, listener, event);
      }
      finally
      {
	ctx.fireContainerEvent(je.getAfter(), listener);
      }
    }
    else
      proceed(manager, listener, event);
  }

  pointcut notifySessionAttribute(Manager manager, HttpSessionAttributeListener listener, HttpSessionBindingEvent event)
    : (execution(void Manager.notifySessionAttributeAdded(HttpSessionAttributeListener, HttpSessionBindingEvent)) ||
       execution(void Manager.notifySessionAttributeRemoved(HttpSessionAttributeListener, HttpSessionBindingEvent)) ||
       execution(void Manager.notifySessionAttributeReplaced(HttpSessionAttributeListener, HttpSessionBindingEvent))) &&
    args(listener, event) &&
    target(manager);

  void
    around(Manager manager, HttpSessionAttributeListener listener, HttpSessionBindingEvent event)
    : notifySessionAttribute(manager, listener, event)
  {
    JumpEntry je=(JumpEntry)_jumpTable.get(thisJoinPointStaticPart.getSignature().getName());

    _log.trace("notifySessionAttribute pointcut");

    if (manager._container instanceof StandardContext)
    {
      StandardContext ctx=(StandardContext)manager._container;
      try
      {
	ctx.fireContainerEvent(je.getBefore(), listener);
	proceed(manager, listener, event);
      }
      finally
      {
	ctx.fireContainerEvent(je.getAfter(), listener);
      }
    }
    else
      proceed(manager, listener, event);
  }
}
