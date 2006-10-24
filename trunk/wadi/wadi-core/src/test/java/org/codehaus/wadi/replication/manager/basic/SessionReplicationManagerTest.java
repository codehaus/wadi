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
import org.codehaus.wadi.web.WebSessionPool;
import org.codehaus.wadi.web.impl.DistributableSession;

import com.agical.rmock.core.match.Expression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class SessionReplicationManagerTest extends RMockTestCase {

    private ReplicationManager repManager;
    private WebSessionPool sessionPool;
    private String key;

    protected void setUp() throws Exception {
        key = "key";
        repManager = (ReplicationManager) mock(ReplicationManager.class);
        sessionPool = (WebSessionPool) mock(WebSessionPool.class);
    }
    
    public void testCreate() throws Exception {
        WebSession session = new DistributableSession(new DummyDistributableSessionConfig());
        
        repManager.create(key, session.getBodyAsByteArray());
        startVerification();
        
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionPool);
        manager.create(key, session);
    }

    public void testUpdate() throws Exception {
        WebSession session = new DistributableSession(new DummyDistributableSessionConfig());
        
        repManager.update(key, session.getBodyAsByteArray());
        startVerification();

        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionPool);
        manager.update(key, session);
    }

    public void testDestroy() {
        repManager.destroy(key);
        startVerification();
        
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionPool);
        manager.destroy(key);
    }

    public void testAcquirePrimary() throws Exception {
        WebSession session = new DistributableSession(new DummyDistributableSessionConfig());
        
        repManager.acquirePrimary(key);
        modify().returnValue(session.getBodyAsByteArray());

        WebSession webSession = sessionPool.take();
        webSession.init(0, 0, 0, key);
        modify().args(new Expression[] {is.ANYTHING, is.ANYTHING, is.ANYTHING, is.AS_RECORDED});
        webSession.setBodyAsByteArray(session.getBodyAsByteArray());
        modify().args(is.AS_RECORDED);
        startVerification();
        
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionPool);
        Object actualSession = manager.acquirePrimary(key);
        assertSame(webSession, actualSession);
    }

    public void testReleasePrimary() {
        ReplicaInfo replicaInfo = new ReplicaInfo(new Object());
        
        repManager.releasePrimary(key);
        startVerification();

        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionPool);
        manager.releasePrimary(key);
    }

    public void testRetrieveReplicaInfo() throws Exception {
        ReplicaInfo replicaInfo = new ReplicaInfo(new Object());
        
        repManager.retrieveReplicaInfo(key);
        modify().returnValue(replicaInfo);
        startVerification();
        
        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionPool);
        ReplicaInfo actualReplicaInfo = manager.retrieveReplicaInfo(key);
        assertSame(replicaInfo, actualReplicaInfo);
    }

    public void testManagePrimary() {
        repManager.managePrimary(key);
        modify().returnValue(true);
        startVerification();

        SessionReplicationManager manager = new SessionReplicationManager(repManager, sessionPool);
        boolean manage = manager.managePrimary(key);
        assertTrue(manage);
    }
    
}
