/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
package org.codehaus.wadi.axis2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.EndPoint;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.PoolableInvocationWrapper;
import org.codehaus.wadi.impl.StatefulHttpServletRequestWrapper;
import EDU.oswego.cs.dl.util.concurrent.Rendezvous;

public class Axis2Invocation implements Invocation, Runnable {
    
    protected final static Log _log=LogFactory.getLog(Invocation.class);
    protected final static ThreadLocal _threadLocalInstance=new ThreadLocal() {protected Object initialValue() {return new Axis2Invocation();}};
    
    public static Axis2Invocation getThreadLocalInstance() {
        return (Axis2Invocation)_threadLocalInstance.get();
    }
    
    protected Manager _wadi;
    protected String _key;
    protected Axis2Session _session;
    
    protected Rendezvous _rendezvous=new Rendezvous(2);
    
    public void init(Manager wadi, String key) {
        _wadi=wadi;
        _key=key;
    }
    
    public void setKey(String key) {
        _key=key;
    }
    
    public void setSession(Session session) {
        _session=(Axis2Session)session;
    }
    
    public Axis2Session getSession() {
        return _session;
    }
    
    public Rendezvous getRendezvous() {
        return _rendezvous;
    }
    
    // Runnable
    
    public void run() {
        // descend contextualiser stack
        // rendezvous with Invocation thread (see invoke())
        // wait for Invocation thread to run through to Axis2Manager.passivateSession()'
        // rendezvous with it again
        // ascend the contextualiser stack
        try {
            _wadi.around(this);
        } catch (InvocationException e) {
            _log.error(e); // FIXME - should be passed back to main thread...
        }
        
        // arriving here :
        // rendezvous with Invocation thread again, allowing it to continue out of container
        try {
            _log.trace(Thread.currentThread().getName()+": Helper thread entering RV[3]");
            _rendezvous.rendezvous(null);
            _log.trace(Thread.currentThread().getName()+": Helper thread leaving RV[3]");
        } catch (InterruptedException e) {
            _log.error(e);
        }
        // finish up...
    }
    
    // Invocation
    
    public void clear() {
        _wadi=null;
        _key=null;
        _rendezvous.restart();
    }
    
    public String getKey() {
        return _key;
    }
    
    public void sendError(int code, String message) {
        _log.error(code+" : "+message);
    }
    
    public boolean getRelocatable() {
        return false;
    }
    
    public void relocate(EndPoint endPoint) {
        throw new UnsupportedOperationException("Axis2 Invocations do not yet support their own relocation");
    }
    
    // we'll just reuse the way that this is done for webcontainers - but, rather than actually wrap our non-existant
    // request with this wrapper, we'll just take the session from it and make that available for the duration of the
    // invocation....
    public void invoke(PoolableInvocationWrapper wrapper) throws InvocationException {
        StatefulHttpServletRequestWrapper w=(StatefulHttpServletRequestWrapper)wrapper; // hacky
        _session=(Axis2Session)w.getSession();
        invoke();
        _session=null;
    }
    
    public void invoke() throws InvocationException {
        Rendezvous rv=_rendezvous;
        // we have just descended the contextualiser stack
        // rendezvous with Invocation thread so that it may continue into the Container
        try {
            _log.trace(Thread.currentThread().getName()+": Helper thread entering RV[1]");
            rv.rendezvous(null);
            _log.trace(Thread.currentThread().getName()+": Helper thread leaving RV[1]");
        } catch (InterruptedException e) {
            _log.error(e);
        }
        // wait, whilst the Invocation thread traverses the container
        // and hits the other side...
        try {
            _log.trace(Thread.currentThread().getName()+": Helper thread entering RV[2]");
            rv.rendezvous(null);
            _log.trace(Thread.currentThread().getName()+": Helper thread leaving RV[2]");
        } catch (InterruptedException e) {
            _log.error(e);
        }
        // continue up the other side of the contextualiser stack...
    }   
    
    public boolean isProxiedInvocation() {
        return false;
    }

}
