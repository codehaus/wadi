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

import java.util.Map;
import java.util.Timer;

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.impl.AbstractMotingContextualiser;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.WebSessionConfig;
import org.codehaus.wadi.web.WebSessionPool;
import org.codehaus.wadi.web.impl.DummyRouter;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
class DummyContextualiserConfig implements ContextualiserConfig {

    protected final Contextualiser _top;
    protected final Map _map;
    protected final Timer _timer=new Timer();
    protected final WebSessionPool _sessionPool;
    protected final WebSessionConfig _sessionConfig=new DummyDistributableSessionConfig();

    public DummyContextualiserConfig(Contextualiser top, Map map, WebSessionPool sessionPool) {
        _top=top;
        _map=map;
        _sessionPool=sessionPool;
        _sessionPool.init(_sessionConfig);
        }

    public int getMaxInactiveInterval() {return 30*60*60;}
    public void expire(Motable motable) {_map.remove(motable.getName());}
    public Immoter getEvictionImmoter() {return ((AbstractMotingContextualiser)_top).getImmoter();} // HACK - FIXME
    public Timer getTimer() {return _timer;}

    public WebSessionPool getSessionPool(){return _sessionPool;}

    protected final Router _router=new DummyRouter();
    public Router getRouter() {return _router;}

    public void notifySessionInsertion(String name) {
    }

    public void notifySessionDeletion(String name) {
    }

    public void notifySessionRelocation(String name) {
    }

}
