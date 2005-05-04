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
package org.codehaus.wadi.sandbox.test;

import java.util.Map;
import java.util.Timer;

import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.ContextualiserConfig;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Motable;
import org.codehaus.wadi.sandbox.Router;
import org.codehaus.wadi.sandbox.SessionConfig;
import org.codehaus.wadi.sandbox.SessionPool;
import org.codehaus.wadi.sandbox.impl.AbstractMotingContextualiser;
import org.codehaus.wadi.sandbox.impl.DistributableSessionFactory;
import org.codehaus.wadi.sandbox.impl.DummyRouter;
import org.codehaus.wadi.sandbox.impl.SimpleSessionPool;

class DummyContextualiserConfig implements ContextualiserConfig {

    protected final Contextualiser _top;
    protected final Map _map;
    protected final Timer _timer=new Timer();
    protected final SessionConfig _sessionConfig=new DummyDistributableSessionConfig();
    protected final SessionPool _sessionPool=new SimpleSessionPool(new DistributableSessionFactory()); // needs init()-ing
    
    public DummyContextualiserConfig(Contextualiser top, Map map) {
        _top=top;
        _map=map;
        _sessionPool.init(_sessionConfig);
        }
    
    public int getMaxInactiveInterval() {return 30*60*60;}
    public void expire(Motable motable) {_map.remove(motable.getId());}
    public Immoter getEvictionImmoter() {return ((AbstractMotingContextualiser)_top).getImmoter();} // HACK - FIXME
    public Timer getTimer() {return _timer;}
    public boolean getAccessOnLoad() {return true;}

    public SessionPool getSessionPool(){return _sessionPool;}
    
    protected final Router _router=new DummyRouter();
    public Router getRouter() {return _router;}

}