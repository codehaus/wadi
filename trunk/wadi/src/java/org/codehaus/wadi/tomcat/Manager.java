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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionListener;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Session;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.util.LifecycleSupport;
import org.codehaus.wadi.shared.Filter;

import javax.servlet.http.HttpSessionEvent;

// TODO
// - all setters need to fire PropertyChange notifications... - should be an aspect
// - register as a listener on our context
// - listen for notifications from our Context and react accordingly
// - throw lifecycle exceptions where necessary

// - need to fire before/afterSessionCreated &
// before/afterSessionDestroyed events for each HttpSessionListener -
// and for AttributeListeners !! - what a waste of time...

public class
  Manager
  extends org.codehaus.wadi.shared.Manager
  implements org.apache.catalina.Manager, org.apache.catalina.Lifecycle
{
  //---------//
  // Manager //
  //---------//

  // new with 5.0.19
  protected int _sessionIdLength=32;
  public int getSessionIdLength(){return _sessionIdLength;}
  public void setSessionIdLength(int length){_sessionIdLength=length;}

  public String getInfo() {return "<code>&lt;org.codehaus.wadi.tomcat.SessionManager&gt;/&lt;1.0b&gt;</code>";}

  protected Container _container;
  public Container getContainer(){return _container;}
  public void setContainer(Container container){_container=container;}

  protected DefaultContext _defaultContext;
  public DefaultContext getDefaultContext(){return _defaultContext;}
  public void setDefaultContext(DefaultContext defaultContext){_defaultContext=defaultContext;}

  // actual notifications are done by aspects...
  protected PropertyChangeSupport _propertyChangeListeners=new PropertyChangeSupport(this);
  public void addPropertyChangeListener(PropertyChangeListener pcl){_propertyChangeListeners.addPropertyChangeListener(pcl);}
  public void removePropertyChangeListener(PropertyChangeListener pcl){_propertyChangeListeners.removePropertyChangeListener(pcl);}

  // actual notifications are done by aspects...
  protected LifecycleSupport _lifecycleListeners=new LifecycleSupport(this);
  public void addLifecycleListener(LifecycleListener ll){_lifecycleListeners.addLifecycleListener(ll);}
  public void removeLifecycleListener(LifecycleListener ll){_lifecycleListeners.removeLifecycleListener(ll);}
  public LifecycleListener[] findLifecycleListeners(){return _lifecycleListeners.findLifecycleListeners();}

  public Session
    createEmptySession()
    {
      return null; // TODO
    }

  public Session
    createSession()
    {
      HttpSessionImpl impl=(HttpSessionImpl)getReadySessionPool().take();
      impl.setManager(this);
      HttpSession facade=(HttpSession)impl.getFacade();
      return facade;
    }

  public void
    add(Session session)
    {
      put(session.getId(), (HttpSessionImpl)session);
    }

  public void
    remove(Session session)
    {
      remove(session.getId());
    }

  public Session
    findSession(String id)
    throws IOException
    {
      HttpSessionImpl impl=(HttpSessionImpl)get(id);
      if (impl==null)
	return null;
      else
      {
	HttpSession facade=(HttpSession) impl.getFacade();
	return facade;
      }
    }

  public Session[]
    findSessions()
    {
      // TODO - caching could optimise this process...
      Collection c=values();
      Session[] sessions=new Session[c.size()];
      int j=0;			// why can't this go in the 'for'
      for (Iterator i=c.iterator();i.hasNext();)
	sessions[j++]=(Session)i.next();
      return sessions;
    }

  public void
    backgroundProcess()
    {
      try
      {
	housekeeper();
      }
      catch (InterruptedException e)
      {
	_log.warn("interrupted", e);
      }
    }

  public void load() throws ClassNotFoundException, IOException {} // TODO
  public void unload() throws IOException {} // TODO

  //-----------//
  // Lifecycle //
  //-----------//

  public synchronized void
    start()
      throws LifecycleException
    {
      try
      {
	super.start();
      }
      catch (Exception e)
      {
	throw new LifecycleException(e);
      }

      if (_container==null)
	throw new LifecycleException("container not yet set");

      Context context=((Context)_container);

      // install filter - TODO - should be below super start
      String filterName="WadiFilter";
      FilterDef fd=new FilterDef();
      fd.setFilterName(filterName);
      fd.setFilterClass(Filter.class.getName());
      context.addFilterDef(fd);
      FilterMap fm=new FilterMap();
      fm.setFilterName(filterName);
      fm.setURLPattern("/*");
      context.addFilterMap(fm);

      // add HttpSessionListeners - we copy them in from our Context -
      // the list should be immutable by now.

      // TODO - this doesn't seem to work :-(
      Object listeners[] = context.getApplicationLifecycleListeners();
      if (listeners!=null)
	for (int i=0; i<listeners.length; i++)
	{
	  Object tmp=listeners[i];
	  if (!(tmp instanceof HttpSessionListener))
	  {
	    _log.trace("adding HttpSessionListener: "+tmp.getClass().getName());
	    _sessionListeners.add(tmp);
	  }
	}
    }

  public synchronized void
    stop()
      throws LifecycleException
    {
      try
      {
	super.stop();
      }
      catch (Exception e)
      {
	throw new LifecycleException(e);
      }
    }

  protected org.codehaus.wadi.shared.Manager.SessionPool _blankSessionPool=new BlankSessionPool();
  protected org.codehaus.wadi.shared.Manager.SessionPool getBlankSessionPool(){return _blankSessionPool;}
  protected void setBlankSessionPool(org.codehaus.wadi.shared.Manager.SessionPool pool){_blankSessionPool=pool;}

  /**
   * A logical pool of uninitialised session impls. Consumes from the
   * ReadyPool and is Consumed by it as session impls are recycled.
   *
   */
  class BlankSessionPool
    extends org.codehaus.wadi.shared.Manager.SessionPool
  {
    public Object take(){return new HttpSessionImpl();}
    public void put(Object o){}	// just let it go
  }

  protected org.codehaus.wadi.shared.HttpSession createFacade(org.codehaus.wadi.shared.HttpSessionImpl impl){return new org.codehaus.wadi.tomcat.HttpSession((org.codehaus.wadi.tomcat.HttpSessionImpl)impl);}

  //----------------------------------------

  // TODO - these need to be hooked up...
  public String getSessionCookieName()  {return "JSESSIONID";}
  public String getSessionCookiePath(HttpServletRequest req){return req.getContextPath();}
  public String getSessionCookieDomain(){return null;}

  public String getSessionUrlParamName(){return "jsessionid";};

  public boolean isServing(InetAddress address, int port){return true;}	// TODO

  public int getHttpPort(){return 8080;} // TODO - temporary hack...

  public ServletContext getServletContext(){return ((Context)_container).getServletContext();}

  //   //--------------------
  //   // session events
  //   //--------------------

  //   // borrowed from tomcat...

  //     /**
  //      * Fire container events if the Context implementation is the
  //      * <code>org.apache.catalina.core.StandardContext</code>.
  //      *
  //      * @param context Context for which to fire events
  //      * @param type Event type
  //      * @param data Event data
  //      *
  //      * @exception Exception occurred during event firing
  //      */
  //   protected void fireContainerEvent(Context context,
  //                                     String type, Object data)
  //     throws Exception
  //     {

  //       if (!"org.apache.catalina.core.StandardContext".equals
  // 	  (context.getClass().getName())) {
  // 	return; // Container events are not supported
  //       }
  //       // NOTE:  Race condition is harmless, so do not synchronize
  //       if (containerEventMethod == null) {
  // 	containerEventMethod =
  // 	  context.getClass().getMethod("fireContainerEvent",
  // 				       containerEventTypes);
  //       }
  //       Object containerEventParams[] = new Object[2];
  //       containerEventParams[0] = type;
  //       containerEventParams[1] = data;
  //       containerEventMethod.invoke(context, containerEventParams);
  //     }

  //   /**
  //    * Notify all session event listeners that a particular event has
  //    * occurred for this Session.  The default implementation performs
  //    * this notification synchronously using the calling thread.
  //    *
  //    * @param type Event type
  //    * @param data Event data
  //    */
  //   public void
  //     fireSessionEvent(String type, Object data)
  //     {
  //       if (listeners.size() < 1)
  // 	return;
  //       SessionEvent event = new SessionEvent(this, type, data);
  //       SessionListener list[] = new SessionListener[0];
  //       synchronized (listeners)
  //       {
  // 	list = (SessionListener[]) listeners.toArray(list);
  //       }

  //       for (int i = 0; i < list.length; i++)
  // 	((SessionListener) list[i]).sessionEvent(event);
  //     }

  //   public void
  //     notifySessionCreated(javax.servlet.http.HttpSession session)
  //     {
  //       // Notify interested session event listeners
  //       fireSessionEvent(Session.SESSION_CREATED_EVENT, null);

  //       // Notify interested application event listeners
  //       Context context=(Context)manager.getContainer();
  //       Object listeners[]=context.getApplicationLifecycleListeners();
  //       if (listeners!=null)
  //       {
  // 	HttpSessionEvent event = new HttpSessionEvent(getSession());
  // 	for (int i = 0; i < listeners.length; i++)
  // 	{
  // 	  Object tmp=listeners[i];
  // 	  if ((tmp instanceof HttpSessionListener))
  // 	    notifySessionCreated((HttpSessionListener)tmp, event);
  // 	}
  //       }
  //     }

  //   public void
  //     notifySessionCreated(HttpSessionListener listener, HttpSessionEvent event)
  //     {
  //       // WTF !!
  //       try
  //       {
  // 	fireContainerEvent(context, "beforeSessionCreated", listener);
  // 	super.notifySessionCreated(listener, event);
  // 	fireContainerEvent(context, "afterSessionCreated", listener);
  //       }
  //       catch (Throwable t)
  //       {
  // 	try
  // 	{
  // 	  fireContainerEvent(context, "afterSessionCreated", listener);
  // 	}
  // 	catch (Exception e)
  // 	{
  // 	  ;
  // 	}
  // 	// FIXME - should we do anything besides log these?
  // 	log(sm.getString("standardSession.sessionEvent"), t);
  //       }
  //     }

  //   public void
  //     notifySessionDestroyed(javax.servlet.http.HttpSession session)
  //   {
  //     int n=_sessionListeners.size();
  //     if (n>0)
  //     {
  //       _log.debug(session.getId()+" : notifying session destruction");
  //       HttpSessionEvent event = new HttpSessionEvent(session);

  //       for(int i=0;i<n;i++)
  // 	((HttpSessionListener)_sessionListeners.get(i)).sessionDestroyed(event);

  //       event=null;
  //     }
  //   }

}
