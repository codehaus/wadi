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

import java.lang.reflect.Method;
import org.apache.catalina.Container;
import org.apache.catalina.DefaultContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO - this aspect relies on a corresponding getter for each setter
// to which it is attached - no getter, no dice. (can we use a
// compile-time aspect to insistthis getter exists?)

// TODO - because this is dynamic (in the way it calculates bean
// getter and event names from the setter name), it is not the fastest
// way to do this, but these setters should be called so infrequently
// that this is not important...

// TODO - it is ugly and prone to fossilisation to define aspects like
// this. In future this should be done by xdoclet - investigate how
// barter.sf.net achieves same result...

public aspect
  PropertyNotification
{
  protected static final Log _log=LogFactory.getLog(PropertyNotification.class);

  pointcut setter(Manager manager)
    : execution(void Manager.setContainer(Container)) && target(manager)
    || execution(void Manager.setDefaultContext(DefaultContext)) && target(manager)
    || execution(void Manager.setSessionIdLength(int)) && target(manager);

  void
    around(Manager manager)
    : setter(manager)
    {
      String setter=thisJoinPointStaticPart.getSignature().getName();

      try
      {
	String getter="g"+setter.substring(1);
	Method m=manager.getClass().getMethod(getter, null);
	Object oldValue=m.invoke(manager, null);
	proceed(manager);
	Object newValue=m.invoke(manager, null);
	// it's probably less work to invoke the getter again, than to
	// construct thisJoinPoint and call getArgs()[0] on it.
	String name=setter.substring(3,4).toLowerCase()+setter.substring(4);
	_log.trace(name+" setter called");
	manager._propertyChangeListeners.firePropertyChange(name, oldValue, newValue);
      }
      catch (Exception e)
      {
	_log.warn("invocation error", e);
      }
    }
}
