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

import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Motable;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class AbstractThinContextualiser implements Contextualiser {

    protected final Contextualiser _next;

    public AbstractThinContextualiser(Contextualiser next) {
        _next=next;
    }
    
    public void evict() {
        // a 'Thin' Contextualiser caches no state...
    }

    public Evicter getEvicter() {return _next.getEvicter();}

    public boolean isLocal() {return _next.isLocal();}

    public Immoter getDemoter(String id, Motable motable) {return _next.getDemoter(id, motable);}
    
    public Immoter getSharedDemoter() {return _next.getSharedDemoter();}

    public void start() throws Exception {
        _next.start();
    }
    
    public void stop() throws Exception {
        _next.stop();
    }
    
}
