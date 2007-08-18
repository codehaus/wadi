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
package org.codehaus.wadi.aop.replication;

import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.aspectj.ClusteredStateAspectUtil;
import org.codehaus.wadi.aop.tracker.basic.BasicInstanceIdFactory;
import org.codehaus.wadi.aop.tracker.basic.BasicInstanceRegistry;
import org.codehaus.wadi.aop.tracker.basic.BasicInstanceTrackerFactory;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.session.ValueFactory;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.replication.manager.ReplicationManager;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ClusteredStateSessionTest extends RMockTestCase {

    private ValueFactory valueFactory;
    private Manager manager;
    private Streamer streamer;
    private ReplicationManager replicationManager;
    private ClusteredStateSession session;
    private DeltaStateHandler serverStateHandler;
    private DeltaStateHandler clientStateHandler;

    @Override
    protected void setUp() throws Exception {
        ClusteredStateAspectUtil.setInstanceTrackerFactory(new BasicInstanceTrackerFactory());

        valueFactory = (ValueFactory) mock(ValueFactory.class);
        manager = (Manager) mock(Manager.class);
        streamer = new SimpleStreamer();
        replicationManager = (ReplicationManager) mock(ReplicationManager.class);
        
        ClusteredStateSessionFactory sessionFactory = new ClusteredStateSessionFactory(
            new ClusteredStateAttributesFactory(valueFactory),
            streamer,
            replicationManager);
        sessionFactory.setManager(manager);
        
        session = sessionFactory.create();
        session.init(1, 2, 3, "name");
        
        clientStateHandler = new DeltaStateHandler(streamer,
            new BasicInstanceIdFactory(),
            new BasicInstanceRegistry());
        clientStateHandler.setObjectFactory(sessionFactory);
        
        serverStateHandler = new DeltaStateHandler(streamer,
            new BasicInstanceIdFactory(),
            new BasicInstanceRegistry());
        serverStateHandler.setObjectFactory(sessionFactory);
    }
    
    public void testRestoreSessionFromFullSerialization() throws Exception {
        Node node = new Node();
        node.value = 1;
        node.child = new Node();
        node.child.value = 2;
        String key = "key";
        session.addState(key, node);
        
        byte[] state = clientStateHandler.extractFullState(session.getName(), session);
     
        ClusteredStateSession restoredSession = 
            (ClusteredStateSession) serverStateHandler.restoreFromFullState(session.getName(), state);
        Node restoredNode = (Node) restoredSession.getState(key);
        assertEquals(node.value, restoredNode.value);

        assertNotNull(restoredNode.child);
        assertEquals(node.child.value, restoredNode.child.value);
        
        assertSessionMetaData(restoredSession);
    }

    public void testRestoreSessionFromFullThenPartialSerialization() throws Exception {
        Node node = new Node();
        node.value = 1;
        node.child = new Node();
        node.child.value = 2;
        String key = "key";
        session.addState(key, node);
        
        byte[] state = clientStateHandler.extractFullState(session.getName(), session);
        clientStateHandler.resetObjectState(session);
        serverStateHandler.restoreFromFullState(session.getName(), state);
        
        session.setLastAccessedTime(10);
        session.setMaxInactiveInterval(20);
        
        node.value = 2;
        node.child.value = 3;

        state = clientStateHandler.extractUpdatedState(session.getName(), session);
        ClusteredStateSession restoredSession = 
            (ClusteredStateSession) serverStateHandler.restoreFromUpdatedState(session.getName(), state);

        Node restoredNode = (Node) restoredSession.getState(key);
        assertEquals(node.value, restoredNode.value);

        assertNotNull(restoredNode.child);
        assertEquals(node.child.value, restoredNode.child.value);
        
        assertSessionMetaData(restoredSession);
    }
    
    protected void assertSessionMetaData(ClusteredStateSession restoredSession) {
        assertEquals(session.getCreationTime(), restoredSession.getCreationTime());
        assertEquals(session.getLastAccessedTime(), restoredSession.getLastAccessedTime());
        assertEquals(session.getMaxInactiveInterval(), restoredSession.getMaxInactiveInterval());
    }

    @ClusteredState
    private static class Node {
        private int value;
        private Node child;
    }
    
}
