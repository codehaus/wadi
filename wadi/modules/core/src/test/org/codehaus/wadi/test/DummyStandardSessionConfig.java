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
package org.codehaus.wadi.test;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Evictable;
import org.codehaus.wadi.EvicterConfig;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.SessionConfig;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.impl.DistributableAttributesFactory;
import org.codehaus.wadi.impl.DistributableValueFactory;
import org.codehaus.wadi.impl.DummyRouter;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.StandardSessionWrapperFactory;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;

class DummyStandardSessionConfig implements SessionConfig {
    
    protected final EvicterConfig _config;
    public DummyStandardSessionConfig(EvicterConfig config) {_config=config;}
    
    protected final ValuePool _valuePool=new SimpleValuePool(new DistributableValueFactory());
    public ValuePool getValuePool() {return _valuePool;}
    
    protected final AttributesFactory _attributesFactory=new DistributableAttributesFactory();
    public AttributesFactory getAttributesFactory() {return _attributesFactory;}
    
    public HttpSessionListener[] getSessionListeners() {return new HttpSessionListener[0];}
    public HttpSessionAttributeListener[] getAttributeListeners() {return new HttpSessionAttributeListener[0];}
    
    public ServletContext getServletContext() {return null;}
    
    public void destroy(Session session) {_config.expire(session);}
    
    protected final SessionWrapperFactory _sessionWrapperfactory=new StandardSessionWrapperFactory();
    public SessionWrapperFactory getSessionWrapperFactory() {return _sessionWrapperfactory;}
    
    protected SessionIdFactory _sessionIdFactory=new TomcatSessionIdFactory();
    public SessionIdFactory getSessionIdFactory() {return _sessionIdFactory;}
    
    protected final int _maxInactiveInterval=2;
    public int getMaxInactiveInterval() {return _maxInactiveInterval;}
    
    public void setLastAccessedTime(Evictable evictable, long oldTime, long newTime) {
        throw new UnsupportedOperationException();
    }
    
    public void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval) {
        throw new UnsupportedOperationException();
    }
    
    protected final Router _router=new DummyRouter();
    public Router getRouter() {return _router;}
    
}