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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO - there are lots more properties that should generate
// notifications on change... - this is really just a proof of concept
// - is there a more generic way that we can do this - it's pretty
// tiresome :-(

// TC carries so much notification baggage in its code that it seemed
// to be begging to have it Aspected out...

public aspect
  PropertyNotification
{
  protected static final Log _log=LogFactory.getLog(PropertyNotification.class);

  pointcut setContainer(Manager manager) : execution(void Manager.setContainer(..)) && target(manager);

  void
    around(Manager manager)
    : setContainer(manager)
    {
      String setter=thisJoinPointStaticPart.getSignature().getName();
      String getter="g"+setter.substring(1);
      String name=setter.substring(3,4).toLowerCase()+setter.substring(4);

      try
      {
	Object oldValue=manager.getClass().getMethod(getter, null).invoke(manager, null);
	proceed(manager);
	Object newValue=manager.getClass().getMethod(getter, null).invoke(manager, null);
	notify(manager, name, oldValue, newValue);
      }
      catch (Exception e)
      {
	_log.warn("invocation error", e);
      }
    }

  pointcut setDefaultContext(Manager manager) : execution(void Manager.setDefaultContext(..)) && target(manager);

  void
    around(Manager manager)
    : setDefaultContext(manager)
    {
      Object oldValue=manager._defaultContext;
      proceed(manager);
      notify(manager, "defaultContext", oldValue, manager._defaultContext);
    }

  pointcut setSessionIdLength(Manager manager) : execution(void Manager.setSessionIdLength(..)) && target(manager);

  void
    around(Manager manager)
    : setSessionIdLength(manager)
    {
      int oldValue=manager._sessionIdLength;
      proceed(manager);
      notify(manager, "sessionIdLength", new Integer(oldValue), new Integer(manager._sessionIdLength));
    }

    public void
      notify(Manager manager, String name, Object oldValue, Object newValue)
    {
      _log.trace(name+" setter called");
      manager._propertyChangeListeners.firePropertyChange(name, oldValue, newValue);
    }
}
