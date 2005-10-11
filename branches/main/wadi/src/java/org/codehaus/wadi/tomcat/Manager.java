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

// TODO
// - all setters need to fire PropertyChange notifications... - should be an aspect
// - register as a listener on our context
// - listen for notifications from our Context and react accordingly
// - throw lifecycle exceptions where necessary

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

  protected PropertyChangeSupport _propertyChangeListeners=new PropertyChangeSupport(this);
  public void addPropertyChangeListener(PropertyChangeListener pcl){_propertyChangeListeners.addPropertyChangeListener(pcl);}
  public void removePropertyChangeListener(PropertyChangeListener pcl){_propertyChangeListeners.removePropertyChangeListener(pcl);}

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

    if (_container!=null)
    {
      Context context=((Context)_container);
      String filterName="WadiFilter";
      FilterDef fd=new FilterDef();
      fd.setFilterName(filterName);
      fd.setFilterClass(Filter.class.getName());
      context.addFilterDef(fd);
      FilterMap fm=new FilterMap();
      fm.setFilterName(filterName);
      fm.setURLPattern("/*");
      context.addFilterMap(fm);
    }

    try
    {
      super.start();
      _lifecycleListeners.fireLifecycleEvent(START_EVENT, null);
    }
    catch (Exception e)
    {
      throw new LifecycleException(e);
    }
  }

  public synchronized void
    stop()
      throws LifecycleException
  {
    try
    {
      super.stop();
      _lifecycleListeners.fireLifecycleEvent(STOP_EVENT, null);
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

}