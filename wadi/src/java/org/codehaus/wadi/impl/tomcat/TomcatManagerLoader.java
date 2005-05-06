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

import org.apache.catalina.Container;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.codehaus.wadi.SpringManagerFactory;

public class TomcatManagerLoader implements Manager, Lifecycle {

  protected final TomcatManager _peer;

  public TomcatManagerLoader() throws Exception {
      super();
      _peer=(TomcatManager)SpringManagerFactory.create("WEB-INF/wadi-tomcat-web.xml", "SessionManager");
  }

  public Container           getContainer()                                                {return _peer.getContainer();}
  public void                setContainer(Container container)                             {_peer.setContainer(container);}
  public DefaultContext      getDefaultContext()                                           {return _peer.getDefaultContext();}
  public void                setDefaultContext(DefaultContext defaultContext)              {_peer.setDefaultContext(defaultContext);}
  public boolean             getDistributable()                                            {return _peer.getDistributable();}
  public void                setDistributable(boolean distributable)                       {_peer.setDistributable(distributable);}
  public String              getInfo()                                                     {return _peer.getInfo();}
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
  public void                start() throws LifecycleException                             {_peer.start();}
  public void                stop() throws LifecycleException                              {_peer.stop();}

}
