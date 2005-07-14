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
package org.codehaus.wadi.impl.tomcat;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;

import org.apache.catalina.Container;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.codehaus.wadi.impl.Filter;
import org.codehaus.wadi.impl.SpringManagerFactory;

public class TomcatManagerLoader implements Manager, Lifecycle {
	
	protected Container _container;
	protected TomcatManager _peer;
	
	public void init(ServletContext context) {
		try {
			InputStream is=context.getResourceAsStream("/WEB-INF/wadi-web.xml");
			_peer=(TomcatManager)SpringManagerFactory.create(is, "SessionManager", new TomcatSessionFactory(), new TomcatSessionWrapperFactory(), new TomcatManagerFactory());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	// PlaceHolder
	public void setFilter(Filter filter) {
		_peer.setFilter(filter);
	}
	
	// org.apache.catalina.Manager
	
	public void setContainer(Container container) {
		_container=container;
	}
	
	public void start() throws LifecycleException {
		StandardContext standardContext=(StandardContext)_container;
		ServletContext servletContext=standardContext.getServletContext();
		init(servletContext);
		_peer.setContainer(_container);
		_peer.start();
	}
	
	public void stop() throws LifecycleException {
		_peer.stop();
	}

  public String              getInfo()                                                     {return _peer.getInfo();}
  public Container           getContainer()                                                {return _peer.getContainer();}
  public DefaultContext      getDefaultContext()                                           {return _peer.getDefaultContext();}
  public void                setDefaultContext(DefaultContext defaultContext)              {_peer.setDefaultContext(defaultContext);}
  public boolean             getDistributable()                                            {return _peer.getDistributable();}
  public void                setDistributable(boolean distributable)                       {_peer.setDistributable(distributable);}
  public int                 getMaxInactiveInterval()                                      {return _peer.getMaxInactiveInterval();}
  public void                setMaxInactiveInterval(int interval)                          {_peer.setMaxInactiveInterval(interval);}
  public int                 getSessionIdLength()                                          {return _peer.getSessionIdLength();}
  public void                setSessionIdLength(int idLength)                              {_peer.setSessionIdLength(idLength);}
  public int                 getSessionCounter()                                           {return _peer.getSessionCounter();}
  public void                setSessionCounter(int sessionCounter)                         {_peer.setSessionCounter(sessionCounter);}
  public int                 getMaxActive()                                                {return _peer.getMaxActive();}
  public void                setMaxActive(int maxActive)                                   {_peer.setMaxActive(maxActive);}
  public int                 getActiveSessions()                                           {return _peer.getActiveSessions();}
  public int                 getExpiredSessions()                                          {return _peer.getExpiredSessions();}
  public void                setExpiredSessions(int expiredSessions)                       {_peer.setExpiredSessions(expiredSessions);}
  public int                 getRejectedSessions()                                         {return _peer.getRejectedSessions();}
  public void                setRejectedSessions(int rejectedSessions)                     {_peer.setRejectedSessions(rejectedSessions);}
  public void                add(Session session)                                          {_peer.add(session);}
  public void                addPropertyChangeListener(PropertyChangeListener listener)    {_peer.addPropertyChangeListener(listener);}
  public Session             createEmptySession()                                          {return _peer.createEmptySession();}
  public Session             createSession()                                               {return _peer.createSession();}
  public Session             findSession(String id) throws IOException                     {return _peer.findSession(id);}
  public Session[]           findSessions()                                                {return _peer.findSessions();}
  public void                load() throws ClassNotFoundException, IOException             {_peer.load();}
  public void                remove(Session session)                                       {_peer.remove(session);}
  public void                removePropertyChangeListener(PropertyChangeListener listener) {_peer.removePropertyChangeListener(listener);}
  public void                unload() throws IOException                                   {_peer.unload();}
  public void                backgroundProcess()                                           {_peer.backgroundProcess();}
  public void                addLifecycleListener(LifecycleListener listener)              {_peer.addLifecycleListener(listener);}
  public LifecycleListener[] findLifecycleListeners()                                      {return _peer.findLifecycleListeners();}
  public void                removeLifecycleListener(LifecycleListener listener)           {_peer.removeLifecycleListener(listener);}

}
