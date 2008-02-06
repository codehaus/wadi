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
package org.codehaus.wadi.replication.contextualiser;

import org.codehaus.wadi.core.contextualiser.DummyContextualiser;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.location.statemanager.StateManager;
import org.codehaus.wadi.replication.contextualiser.ReplicaAwareContextualiser;
import org.codehaus.wadi.replication.manager.ReplicationManager;

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
        Motable emotable = (Motable) mock(Motable.class);
        emotable.getCreationTime();
        int creationTime = 1;
        modify().returnValue(creationTime);
        emotable.getLastAccessedTime();
        int lastAccessedTime = 2;
        modify().returnValue(lastAccessedTime);
        emotable.getMaxInactiveInterval();
        int maxInactiveInterval = 3;
        modify().returnValue(maxInactiveInterval);
        emotable.getName();
        String name = "name";
        modify().returnValue(name);
        emotable.getBodyAsByteArray();
        byte[] body = new byte[0];
        modify().returnValue(body);
        
        Motable immotable = (Motable) mock(Motable.class);
        immotable.restore(creationTime, lastAccessedTime, maxInactiveInterval, name, body);
        
        startVerification();
        
        ReplicaAwareContextualiser contextualiser = new ReplicaAwareContextualiser(new DummyContextualiser(stateManager),
                manager,
                stateManager);
        
        Emoter emoter = contextualiser.getEmoter();
        emoter.emote(emotable, immotable);
    }

    public void testGet() throws Exception {
        Motable motable = (Motable) mock(Motable.class);
        motable.getName();
        String key = "id";
        modify().returnValue(key);
        manager.retrieveReplica(key);
        modify().returnValue(motable);
        
        stateManager.insert(key);
        startVerification();
        
        ReplicaAwareContextualiser contextualiser = new ReplicaAwareContextualiser(new DummyContextualiser(stateManager),
                manager,
                stateManager);
        Motable actualMotable = contextualiser.get(key, false);
        assertSame(motable, actualMotable);
    }
    
    
}
