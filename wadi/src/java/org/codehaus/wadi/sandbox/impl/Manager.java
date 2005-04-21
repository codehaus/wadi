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
package org.codehaus.wadi.sandbox.impl;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.IdGenerator;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Session;
import org.codehaus.wadi.sandbox.SessionWrapperFactory;
import org.codehaus.wadi.sandbox.ValuePool;
import org.codehaus.wadi.sandbox.AttributesPool;
import org.codehaus.wadi.sandbox.SessionConfig;
import org.codehaus.wadi.sandbox.SessionPool;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class Manager implements SessionConfig {

    protected final Log _log = LogFactory.getLog(getClass());

    protected final SessionPool _sessionPool;
    protected final AttributesPool _attributesPool;
    protected final ValuePool _valuePool;
    protected final SessionWrapperFactory _sessionWrapperFactory;
    protected final IdGenerator _sessionIdFactory;
    protected final Contextualiser _contextualiser;
    protected final Map _map;

    public Manager(SessionPool sessionPool, AttributesPool attributesPool, ValuePool valuePool, SessionWrapperFactory sessionWrapperFactory, IdGenerator sessionIdFactory, Contextualiser contextualiser, Map map) {
        _sessionPool=sessionPool;
        _sessionPool.init(this); // set up a backptr - yeugh !
        _attributesPool=attributesPool;
        _valuePool=valuePool;
        _sessionWrapperFactory=sessionWrapperFactory;
        _sessionIdFactory=sessionIdFactory;
        _contextualiser=contextualiser;
        _map=map;
    }
    
    public boolean isStarted(){return false;}
    
    public void destroySession(Session session) {
        for (Iterator i=new ArrayList(session.getAttributeNameSet()).iterator(); i.hasNext();) // ALLOC ?
            session.removeAttribute((String)i.next()); // TODO - very inefficient
        String id=session.getId();
        _map.remove(id);
        // _sessionIdFactory.put(id); // we might reuse session ids ? - sounds like a BAD idea
        session.destroy();
        _sessionPool.put(session);
    }

    public Session createSession() {
        Session session=_sessionPool.take();
        String id=(String)_sessionIdFactory.take(); // TODO - API on this class is wrong...
        session.setId(id);
        _map.put(id, session);
        return session;
    }
    
    //----------------------------------------
    // Listeners

    // These lists are only modified at webapp [un]deployment time, by a
    // single thread, so although read by multiple threads whilst the
    // Manager is running, need no synchronization...

    protected final List _sessionListeners  =new ArrayList();
    public List getSessionListeners(){return _sessionListeners;}
    protected final List _attributeListeners=new ArrayList();
    public List getAttributeListeners(){return _attributeListeners;}

    public synchronized void
      addEventListener(EventListener listener)
        throws IllegalArgumentException, IllegalStateException
    {
      if (isStarted())
        throw new IllegalStateException("EventListeners must be added before a Session Manager starts");

      boolean known=false;
      if (listener instanceof HttpSessionAttributeListener)
      {
        if (_log.isDebugEnabled()) _log.debug("adding HttpSessionAttributeListener: "+listener);
        _attributeListeners.add(listener);
        known=true;
      }
      if (listener instanceof HttpSessionListener)
      {
        if (_log.isDebugEnabled()) _log.debug("adding HttpSessionListener: "+listener);
        _sessionListeners.add(listener);
        known=true;
      }

      if (!known)
        throw new IllegalArgumentException("Unknown EventListener type "+listener);
    }

    public synchronized void
      removeEventListener(EventListener listener)
        throws IllegalStateException
    {
      boolean known=false;

      if (isStarted())
        throw new IllegalStateException("EventListeners may not be removed while a Session Manager is running");

      if (listener instanceof HttpSessionAttributeListener)
      {
        if (_log.isDebugEnabled()) _log.debug("removing HttpSessionAttributeListener: "+listener);
        known|=_attributeListeners.remove(listener);
      }
      if (listener instanceof HttpSessionListener)
      {
        if (_log.isDebugEnabled()) _log.debug("removing HttpSessionListener: "+listener);
        known|=_sessionListeners.remove(listener);
      }

      if (!known)
        if (_log.isWarnEnabled()) _log.warn("EventListener not registered: "+listener);
    }
    
    // Context stuff
    public ServletContext getServletContext() {return null;}// TODO
    
    public AttributesPool getAttributesPool() {return _attributesPool;}
    public ValuePool getValuePool() {return _valuePool;}
    
    public Manager getManager(){return this;}
    
    // this should really be abstract, but is useful for testing - TODO

    public SessionWrapperFactory getSessionWrapperFactory() {return _sessionWrapperFactory;}
    
    public IdGenerator getSessionIdFactory() {return _sessionIdFactory;}
    
    protected int _maxInactiveInterval=30*60;
    public int getMaxInactiveInterval(){return _maxInactiveInterval;}
    public void setMaxInactiveInterval(int interval){_maxInactiveInterval=interval;}
    
    // integrate with Filter instance
    protected Filter _filter;

    public void setFilter(Filter filter){_filter=filter;}
    
    public boolean getDistributable(){return false;}

    public Contextualiser getContextualiser() {return _contextualiser;}

}
