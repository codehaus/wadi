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

import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.test.DummyDistributableSessionConfig;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.impl.DistributableSession;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class SessionReplicationManagerTest extends RMockTestCase {

    private ReplicationManager repManager;
    private SessionRehydrater sessionRehydrater;
    private String key;

    protected void setUp() throws Exception {
        key = "key";
        repManager = (ReplicationManager) mock(ReplicationManager.class);
        sessionRehydrater = (SessionRehydrater) mock(SessionRehydrater.class);
    }
    
    public void testCreate() throws Exception {
        WebSession session = new DistributableSession(new DummyDistributableSessionConfig());
        
        repManager.create(key, session.getBodyAsByteArray());
        startVerification();
        
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        manager.create(key, session);
    }

    public void testUpdate() throws Exception {
        WebSession session = new DistributableSession(new DummyDistributableSessionConfig());
        
        repManager.update(key, session.getBodyAsByteArray());
        startVerification();

        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        manager.update(key, session);
    }

    public void testDestroy() {
        repManager.destroy(key);
        startVerification();
        
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        manager.destroy(key);
    }

    public void testAcquirePrimary() throws Exception {
        WebSession session = new DistributableSession(new DummyDistributableSessionConfig());
        
        repManager.acquirePrimary(key);
        modify().returnValue(session.getBodyAsByteArray());

        sessionRehydrater.rehydrate(key, session.getBodyAsByteArray());
        modify().returnValue(session);
        startVerification();
        
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        Object actualSession = manager.acquirePrimary(key);
        assertSame(session, actualSession);
    }

    public void testReleasePrimary() {
        ReplicaInfo replicaInfo = new ReplicaInfo(new Object());
        
        repManager.releasePrimary(key);
        startVerification();

        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        manager.releasePrimary(key);
    }

    public void testRetrieveReplicaInfo() {
        ReplicaInfo replicaInfo = new ReplicaInfo(new Object());
        
        repManager.retrieveReplicaInfo(key);
        modify().returnValue(replicaInfo);
        startVerification();
        
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        ReplicaInfo actualReplicaInfo = manager.retrieveReplicaInfo(key);
        assertSame(replicaInfo, actualReplicaInfo);
    }

    public void testManagePrimary() {
        repManager.managePrimary(key);
        modify().returnValue(true);
        startVerification();

        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionRehydrater);
        boolean manage = manager.managePrimary(key);
        assertTrue(manage);
    }
    
}
