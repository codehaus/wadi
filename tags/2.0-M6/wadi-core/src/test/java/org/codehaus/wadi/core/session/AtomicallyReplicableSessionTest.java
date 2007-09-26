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
package org.codehaus.wadi.core.session;

import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.replication.manager.ReplicationManager;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class AtomicallyReplicableSessionTest extends RMockTestCase {

    private DistributableAttributes attributes;
    private Manager manager;
    private ReplicationManager replicationManager;
    private Streamer streamer;
    private AtomicallyReplicableSession session;
    private String sessionName;

    protected void setUp() throws Exception {
        attributes = new DistributableAttributes(new DistributableValueFactory(new BasicValueHelperRegistry()));
        manager = (Manager) mock(Manager.class);
        replicationManager = (ReplicationManager) mock(ReplicationManager.class);
        streamer = (Streamer) mock(Streamer.class);
        session = new AtomicallyReplicableSession(attributes, manager, streamer, replicationManager);
        sessionName = "name";
        session.init(1, 1, 1, sessionName);
    }
    
    public void testCreateReplicaIfNew() throws Exception {
        replicationManager.create(sessionName, session);
        startVerification();
        
        session.onEndProcessing();
    }

    public void testUpdateReplicaIfDirty() throws Exception {
        replicationManager.create(sessionName, session);
        replicationManager.update(sessionName, session);
        startVerification();
        
        session.onEndProcessing();
        session.addState("key", "value");
        session.onEndProcessing();
    }

    public void testAddStateSetDirty() throws Exception {
        replicationManager.create(sessionName, session);
        replicationManager.update(sessionName, session);
        startVerification();
        
        session.onEndProcessing();
        session.addState("key", "value");
        session.onEndProcessing();
    }
    
    public void testRemoveStateSetDirty() throws Exception {
        replicationManager.create(sessionName, session);
        replicationManager.update(sessionName, session);
        startVerification();
        
        session.onEndProcessing();
        session.removeState("key");
        session.onEndProcessing();
    }
    
    public void testGetStateSetDirty() throws Exception {
        replicationManager.create(sessionName, session);
        replicationManager.update(sessionName, session);
        replicationManager.update(sessionName, session);
        startVerification();

        session.onEndProcessing();

        String key = "key";
        session.addState(key, "value");
        session.onEndProcessing();
        
        session.getState(key);
        session.onEndProcessing();
    }
    
}
