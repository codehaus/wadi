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

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMPeer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class MotableReplicationManagerTest extends RMockTestCase {

    private ReplicationManager repManager;
    private String key;
    private Motable motable;
    private byte[] webSessionBody = new byte[0];

    protected void setUp() throws Exception {
        key = "key";
        repManager = (ReplicationManager) mock(ReplicationManager.class);
        
        motable = (Motable) mock(Motable.class);
        motable.getBodyAsByteArray();
        modify().multiplicity(expect.from(0)).returnValue(webSessionBody);
    }
    
    public void testCreate() throws Exception {
        repManager.create(key, webSessionBody);
        startVerification();
        
        MotableReplicationManager manager = new MotableReplicationManager(repManager);
        manager.create(key, motable);
    }

    public void testUpdate() throws Exception {
        repManager.update(key, webSessionBody);
        startVerification();

        MotableReplicationManager manager = new MotableReplicationManager(repManager);
        manager.update(key, motable);
    }

    public void testDestroy() {
        repManager.destroy(key);
        startVerification();
        
        MotableReplicationManager manager = new MotableReplicationManager(repManager);
        manager.destroy(key);
    }

    public void testAcquirePrimary() throws Exception {
        repManager.acquirePrimary(key);
        modify().returnValue(webSessionBody);
        startVerification();
        
        MotableReplicationManager manager = new MotableReplicationManager(repManager);
        Motable actualMotable = (Motable) manager.acquirePrimary(key);
        assertSame(webSessionBody, actualMotable.getBodyAsByteArray());
    }

    public void testReleasePrimary() {
        repManager.releasePrimary(key);
        startVerification();

        MotableReplicationManager manager = new MotableReplicationManager(repManager);
        manager.releasePrimary(key);
    }

    public void testRetrieveReplicaInfo() throws Exception {
        ReplicaInfo replicaInfo = new ReplicaInfo(new VMPeer("peer1", null), new Peer[0], new Object());
        
        repManager.retrieveReplicaInfo(key);
        modify().returnValue(replicaInfo);
        startVerification();
        
        MotableReplicationManager manager = new MotableReplicationManager(repManager);
        ReplicaInfo actualReplicaInfo = manager.retrieveReplicaInfo(key);
        assertSame(replicaInfo, actualReplicaInfo);
    }

    public void testManagePrimary() {
        repManager.managePrimary(key);
        modify().returnValue(true);
        startVerification();

        MotableReplicationManager manager = new MotableReplicationManager(repManager);
        boolean manage = manager.managePrimary(key);
        assertTrue(manage);
    }

}
