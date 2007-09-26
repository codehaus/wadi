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

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.eviction.Evicter;
import org.codehaus.wadi.core.manager.SessionMonitor;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.session.SessionFactory;

import com.agical.rmock.core.match.Expression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class MemoryContextualiserTest extends RMockTestCase {

    public void testImmotion() throws Exception {
        Contextualiser next = (Contextualiser) mock(Contextualiser.class);
        Evicter evicter = (Evicter) mock(Evicter.class);
        ConcurrentMotableMap map = (ConcurrentMotableMap) mock(ConcurrentMotableMap.class);
        SessionFactory sessionFactory = (SessionFactory) mock(SessionFactory.class);
        InvocationContextFactory contextFactory = (InvocationContextFactory) mock(InvocationContextFactory.class);
        SessionMonitor sessionMonitor = (SessionMonitor) mock(SessionMonitor.class);
        
        Motable motable = (Motable) mock(Motable.class);
        String name = "name";
        motable.getName();
        modify().returnValue(name);

        motable.getTimeToLive(0);
        modify().args(is.ANYTHING).returnValue(10);
        evicter.testForDemotion(motable, 0, 10);
        modify().args(new Expression[] {is.AS_RECORDED, is.ANYTHING, is.AS_RECORDED}).returnValue(false);
        
        Session session = sessionFactory.create();
        map.put(name, session);
        sessionMonitor.notifyInboundSessionMigration(session);
        
        startVerification();
        
        MemoryContextualiser contextualiser = new MemoryContextualiser(next,
                evicter,
                map,
                sessionFactory,
                contextFactory,
                sessionMonitor);
        
        Immoter immoter = contextualiser.getDemoter(name, motable);
        Motable newMotable = immoter.newMotable(motable);
        boolean success = immoter.immote(motable, newMotable);
        assertTrue(success);
    }
    
}
