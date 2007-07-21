/**
 * Copyright 2007 The Apache Software Foundation
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
package org.codehaus.wadi.core.contextualiser;

import org.codehaus.wadi.core.manager.SessionMonitor;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.store.Store;
import org.codehaus.wadi.core.store.Store.Putter;
import org.codehaus.wadi.location.statemanager.StateManager;

import com.agical.rmock.core.Action;
import com.agical.rmock.core.MethodHandle;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class SharedStoreContextualiserTest extends RMockTestCase {

    public void testStateManagerIsNotifiedOnLoad() throws Exception {
        Contextualiser next = (Contextualiser) mock(Contextualiser.class);
        Store store = (Store) mock(Store.class);
        StateManager stateManager = (StateManager) mock(StateManager.class);
        SessionMonitor sessionMonitor = (SessionMonitor) mock(SessionMonitor.class);
        Emoter emoter = (Emoter) mock(Emoter.class);
        Immoter immoter = (Immoter) mock(Immoter.class);
        
        final Motable motable = (Motable) mock(Motable.class);
        final String name = "name";
        store.load(null);
        modify().args(is.ANYTHING).perform(new Action() {
            public Object invocation(Object[] arg0, MethodHandle arg1) throws Throwable {
                Store.Putter putter = (Putter) arg0[0];
                putter.put(name, motable);
                return null;
            }
        });

        stateManager.insert(name);
        
        Session newSession = (Session) mock(Session.class);
        immoter.newMotable(motable);
        modify().returnValue(newSession);
        emoter.emote(motable, newSession);
        modify().returnValue(true);
        immoter.immote(motable, newSession);
        modify().returnValue(true);
        
        sessionMonitor.notifySessionCreation(newSession);
        
        startVerification();
        
        SharedStoreContextualiser contextualiser = new SharedStoreContextualiser(next,
                store,
                stateManager,
                sessionMonitor);
        contextualiser.load(emoter, immoter);
    }
    
}
