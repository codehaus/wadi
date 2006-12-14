/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.replication.contextualizer;

import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.location.StateManager;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.test.DummyDistributableSessionConfig;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.impl.DistributableSession;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ReplicaAwareContextualiserTest extends RMockTestCase {
    private ReplicationManager manager;
    private StateManager stateManager;

    protected void setUp() throws Exception {
        manager = (ReplicationManager) mock(ReplicationManager.class);
        stateManager = (StateManager) mock(StateManager.class);
    }
    
    public void testEmoter() throws Exception {
        ReplicaAwareContextualiser contextualiser = new ReplicaAwareContextualiser(new DummyContextualiser(),
                manager,
                stateManager);
        
        startVerification();
        
        Emoter emoter = contextualiser.getEmoter();
        
        WebSession emotable = new DistributableSession(new DummyDistributableSessionConfig());
        emotable.init(1, 2, 3, "name");
        String attrKey = "attrKey";
        String attrValue = "attrValue";
        emotable.setAttribute(attrKey, attrValue);
        WebSession immotable = new DistributableSession(new DummyDistributableSessionConfig());
        emoter.emote(emotable, immotable);
        
        assertEquals(emotable.getCreationTime(), immotable.getCreationTime());
        assertEquals(emotable.getLastAccessedTime(), immotable.getLastAccessedTime());
        assertEquals(emotable.getMaxInactiveInterval(), immotable.getMaxInactiveInterval());
        assertEquals(attrValue, immotable.getAttribute(attrKey));
    }

    public void testGet() throws Exception {
        WebSession motable = new DistributableSession(new DummyDistributableSessionConfig());
        String key = "id";
        motable.init(1, 2, 3, key);
        
        manager.acquirePrimary(key);
        modify().returnValue(motable);
        
        stateManager.relocate(key);
        
        startVerification();
        
        ReplicaAwareContextualiser contextualiser = new ReplicaAwareContextualiser(new DummyContextualiser(),
                manager,
                stateManager);
        Motable actualMotable = contextualiser.get(key, false);
        assertSame(motable, actualMotable);
    }
    
    
}
