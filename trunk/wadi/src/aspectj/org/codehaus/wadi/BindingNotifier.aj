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

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Notifies HttpSessionBindingListeners of session mutation
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1.4 $
 */
public aspect
  BindingNotifier
{
  private static final Log _log=LogFactory.getLog(BindingNotifier.class);

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
	  javax.servlet.http.HttpSession facade=ahsi.getFacade();
	  notifyValueUnbound(ahsi.getRealId(), facade, key, oldVal);
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
      String realId=ahsi.getRealId();
      javax.servlet.http.HttpSession facade=ahsi.getFacade();

      // send binding notifications
      try
      {
	if (oldVal!=null)
	  notifyValueUnbound(realId, facade, key, oldVal);

	notifyValueBound(realId, facade, key, val);
      }
      catch (Throwable t)
      {
	_log.error("error in user owned Listener - notifications may be incomplete", t);
      }

      return returnVal?oldVal:null;
    }

  // impl

  protected void
    notifyValueUnbound(String realId, javax.servlet.http.HttpSession session, String key, Object val)
    {
      if (val instanceof HttpSessionBindingListener)
      {
	_log.trace(realId+" : notifying unbinding(\""+key+"\") : "+val);

	HttpSessionBindingEvent event=new HttpSessionBindingEvent(session, key, val);
	((HttpSessionBindingListener)val).valueUnbound(event);
	event=null;
      }
    }

  protected void
    notifyValueBound(String realId, javax.servlet.http.HttpSession session, String key, Object val)
    {
      if (val instanceof HttpSessionBindingListener)
      {
	_log.trace(realId+": notifying binding(\""+key+"\") : "+val);

	HttpSessionBindingEvent event=new HttpSessionBindingEvent(session, key, val);
	((HttpSessionBindingListener)val).valueBound(event);
	event=null;
      }
    }
}
