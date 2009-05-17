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
package org.codehaus.wadi.core.manager;

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.contextualiser.InvocationProxy;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.location.partitionmanager.PartitionManager;
import org.codehaus.wadi.location.statemanager.StateManager;

import com.agical.rmock.extension.junit.RMockTestCase;


/**
 * 
 * @version $Revision: 1538 $
 */
public class ClusteredManagerTest extends RMockTestCase {

    public void testDestroy() throws Exception {
        StateManager stateManager = (StateManager) mock(StateManager.class);
        PartitionManager partitionManager = (PartitionManager) mock(PartitionManager.class);
        SessionFactory sessionFactory = (SessionFactory) mock(SessionFactory.class); 
        SessionIdFactory sessionIdFactory = (SessionIdFactory) mock(SessionIdFactory.class);
        Contextualiser contextualiser = (Contextualiser) mock(Contextualiser.class); 
        ConcurrentMotableMap sessionMap = (ConcurrentMotableMap) mock(ConcurrentMotableMap.class);
        Router router = (Router) mock(Router.class);
        SessionMonitor sessionMonitor = (SessionMonitor) mock(SessionMonitor.class);
        InvocationProxy proxy = (InvocationProxy) mock(InvocationProxy.class);
        Session session = (Session) mock(Session.class);
        String name = "name";
        session.getName();
        modify().multiplicity(expect.from(0)).returnValue(name);
        
        sessionFactory.setManager(null);
        modify().args(is.NOT_NULL);
        sessionMap.remove(name);
        sessionMonitor.notifySessionDestruction(session);
        stateManager.remove(name);
        startVerification();
        
        ClusteredManager manager = new ClusteredManager(stateManager,
                partitionManager,
                sessionFactory,
                sessionIdFactory,
                contextualiser,
                sessionMap,
                router,
                sessionMonitor,
                proxy);
        manager.destroy(session);
    }
    
}
