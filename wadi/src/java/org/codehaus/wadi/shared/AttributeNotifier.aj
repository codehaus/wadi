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

import java.util.List;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public aspect
  AttributeNotifier
{
  private static final Log _log=LogFactory.getLog(AttributeNotifier.class);

  pointcut removeAttribute(AbstractHttpSessionImpl ahsi, String key, boolean returnVal) :
    execution(Object HttpSessionSetters.removeAttribute(String, boolean)) && args(key, returnVal) && target(ahsi);

  Object
    around(AbstractHttpSessionImpl ahsi, String key, boolean returnVal)
    : removeAttribute(ahsi, key, returnVal)
    {
      Object oldVal=proceed(ahsi, key, true);

      if (oldVal!=null)
      {
	try
	{
	  ahsi.getWadiManager().notifySessionAttributeRemoved(ahsi.getFacade(), key, oldVal);
	}
	catch (Throwable t)
	{
	  _log.error("error in user owned Listener - notifications may be incomplete", t);
	}
      }

      return returnVal?oldVal:null;
    }

  pointcut setAttribute(AbstractHttpSessionImpl ahsi, String key, Object val, boolean returnVal) :
    execution(Object HttpSessionSetters.setAttribute(String, Object, boolean)) && args(key, val, returnVal) && target(ahsi);

  Object
    around(AbstractHttpSessionImpl ahsi, String key, Object val, boolean returnVal)
    : setAttribute(ahsi, key, val, returnVal)
    {
      Object oldVal=proceed(ahsi, key, val, true);
      javax.servlet.http.HttpSession facade=ahsi.getFacade();

      // send binding notifications
      try
      {
	// send attribute notifications
	if (oldVal!=null)
	  ahsi.getWadiManager().notifySessionAttributeReplaced(facade, key, oldVal, val);
	else
	  ahsi.getWadiManager().notifySessionAttributeAdded(facade, key, val);
      }
      catch (Throwable t)
      {
	_log.error("error in user owned Listener - notifications may be incomplete", t);
      }

      return returnVal?oldVal:null;
    }
}
