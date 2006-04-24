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
package org.codehaus.wadi.replication.manager.basic;

import junit.framework.TestCase;

import org.codehaus.wadi.RehydrationException;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.impl.DistributableSession;
import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.BaseMockReplicationManager;
import org.codehaus.wadi.test.DummyDistributableSessionConfig;

/**
 * 
 * @version $Revision: 1538 $
 */
public class SessionReplicationManagerTest extends TestCase {

    public void testCreate() {
        final Object[] parameters = new Object[2];
        BaseMockReplicationManager repManager = new BaseMockReplicationManager() {
            public void create(Object key, Object tmp) {
                parameters[0] = key;
                parameters[1] = tmp;
            };
        };

        MockSessionRehydrater sessionRehydrater = new MockSessionRehydrater();
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        
        String key = "key";
        Session session = new DistributableSession(new DummyDistributableSessionConfig());
        manager.create(key, session);

        assertSame(key, parameters[0]);
        assertNotNull(parameters[1]);
    }

    public void testUpdate() {
        final Object[] parameters = new Object[2];
        BaseMockReplicationManager repManager = new BaseMockReplicationManager() {
            public void update(Object key, Object tmp) {
                parameters[0] = key;
                parameters[1] = tmp;
            };
        };

        MockSessionRehydrater sessionRehydrater = new MockSessionRehydrater();
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        
        String key = "key";
        Session session = new DistributableSession(new DummyDistributableSessionConfig());
        manager.update(key, session);

        assertSame(key, parameters[0]);
        assertNotNull(parameters[1]);
    }

    public void testDestroy() {
        final Object[] parameters = new Object[1];
        BaseMockReplicationManager repManager = new BaseMockReplicationManager() {
            public void destroy(Object key) {
                parameters[0] = key;
            };
        };

        MockSessionRehydrater sessionRehydrater = new MockSessionRehydrater();
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        
        String key = "key";
        manager.destroy(key);

        assertSame(key, parameters[0]);
    }

    public void testAcquirePrimary() throws Exception {
        Session session = new DistributableSession(new DummyDistributableSessionConfig());
        
        final Object[] parameters = new Object[1];
        final Object returned = session.getBodyAsByteArray();
        BaseMockReplicationManager repManager = new BaseMockReplicationManager() {
            public Object acquirePrimary(Object key) {
                parameters[0] = key;
                return returned;
            };
        };

        MockSessionRehydrater sessionRehydrater = new MockSessionRehydrater();
        sessionRehydrater.session = session;
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        
        String key = "key";
        Object actualSession = manager.acquirePrimary(key);

        assertSame(key, parameters[0]);
        assertSame(session, actualSession);
    }

    public void testReleasePrimary() {
        final ReplicaInfo replicaInfo = new ReplicaInfo((NodeInfo) null, null, null);
        final Object[] parameters = new Object[1];
        BaseMockReplicationManager repManager = new BaseMockReplicationManager() {
            public ReplicaInfo releasePrimary(Object key) {
                parameters[0] = key;
                return replicaInfo;
            };
        };

        MockSessionRehydrater sessionRehydrater = new MockSessionRehydrater();
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        
        String key = "key";
        ReplicaInfo actualReplicaInfo = manager.releasePrimary(key);

        assertSame(key, parameters[0]);
        assertSame(replicaInfo, actualReplicaInfo);
    }

    public void testRetrieveReplicaInfo() {
        final ReplicaInfo replicaInfo = new ReplicaInfo((NodeInfo) null, null, null);
        final Object[] parameters = new Object[1];
        BaseMockReplicationManager repManager = new BaseMockReplicationManager() {
            public ReplicaInfo retrieveReplicaInfo(Object key) {
                parameters[0] = key;
                return replicaInfo;
            };
        };

        MockSessionRehydrater sessionRehydrater = new MockSessionRehydrater();
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        
        String key = "key";
        ReplicaInfo actualReplicaInfo = manager.retrieveReplicaInfo(key);

        assertSame(key, parameters[0]);
        assertSame(replicaInfo, actualReplicaInfo);
    }

    public void testManagePrimary() {
        final Object[] parameters = new Object[1];
        BaseMockReplicationManager repManager = new BaseMockReplicationManager() {
            public boolean managePrimary(Object key) {
                parameters[0] = key;
                return true;
            };
        };

        MockSessionRehydrater sessionRehydrater = new MockSessionRehydrater();
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        
        String key = "key";
        boolean manage = manager.managePrimary(key);

        assertSame(key, parameters[0]);
        assertTrue(manage);
    }
    
    public class MockSessionRehydrater implements SessionRehydrater {
        private byte[] body;
        private String key;
        private Session session;
        
        public Session rehydrate(String key, byte[] body) throws RehydrationException {
            this.key = key;
            this.body = body;
            return session;
        }
    }
}
