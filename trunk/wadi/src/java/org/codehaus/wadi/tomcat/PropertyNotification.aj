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
import java.util.HashMap;
import java.util.Map;
import org.apache.catalina.Container;
import org.apache.catalina.DefaultContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO - this aspect relies on a corresponding getter for each setter
// to which it is attached - no getter, no dice. (can we use a
// compile-time aspect to insist this getter exists?)

// TODO - it is ugly and prone to fossilisation to define aspects like
// this. In future this should be done by xdoclet - investigate how
// barter.sf.net achieves same result...

// NB - although, from looking at Tomcat code it seems that we should
// also support this for HttpSession.setAuthType() and setPrincipal(),
// I can see no reason to do this as there appears to be no mechanism
// for registering a listener for these events...

public aspect
  PropertyNotification
{
  protected static final Log _log=LogFactory.getLog(PropertyNotification.class);

  pointcut setter(Manager manager)
    : (execution(void Manager.setContainer(Container)) ||
       execution(void Manager.setDefaultContext(DefaultContext)) ||
       execution(void Manager.setSessionIdLength(int))) &&
    target(manager);


  static class
    JumpEntry
  {
    String _name;
    Method _getter;

    JumpEntry(Class clazz, String name)
      throws NoSuchMethodException
    {
      _name=name;
      String suffix=_name.substring(0,1).toUpperCase()+_name.substring(1);
      _getter=clazz.getMethod("get"+suffix, null);
    }

    Method getGetter(){return _getter;}
    String getName(){return _name;}
  }

  static final Map _jumpTable=new HashMap();
  static
  {
    try
    {
      _jumpTable.put("setContainer"       , new JumpEntry(Manager.class, "container"));
      _jumpTable.put("setDefaultContext"  , new JumpEntry(Manager.class, "defaultContext"));
      _jumpTable.put("setSessionIdLength" , new JumpEntry(Manager.class, "sessionIdLength"));
    }
    catch (Exception e)
    {
      _log.error("jump table initialisation failed", e);
    }
  }

  void
    around(Manager manager)
    : setter(manager)
    {
      JumpEntry je=(JumpEntry)_jumpTable.get(thisJoinPointStaticPart.getSignature().getName());

      try
      {
	Method getter=je.getGetter();
	Object oldValue=getter.invoke(manager, null);
	proceed(manager);
	Object newValue=getter.invoke(manager, null);
	// it's probably less work to invoke the getter again, than to
	// construct thisJoinPoint and call getArgs()[0] on it.
	String name=je.getName();
	_log.trace("bound property modified: "+name);
	manager._propertyChangeListeners.firePropertyChange(name, oldValue, newValue);
      }
      catch (Exception e)
      {
	_log.warn("invocation error", e);
      }
    }
}
