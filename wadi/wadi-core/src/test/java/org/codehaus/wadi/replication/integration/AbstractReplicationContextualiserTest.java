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
package org.codehaus.wadi.replication.integration;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.TestCase;

import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.ContextPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.RWLockListener;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.gridstate.impl.DummyPartitionManager;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.AlwaysEvicter;
import org.codehaus.wadi.impl.AtomicallyReplicableSessionFactory;
import org.codehaus.wadi.impl.ClusteredManager;
import org.codehaus.wadi.impl.DistributableAttributesFactory;
import org.codehaus.wadi.impl.DistributableValueFactory;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyManagerConfig;
import org.codehaus.wadi.impl.DummyRouter;
import org.codehaus.wadi.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.impl.HashingCollapser;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.StandardHttpProxy;
import org.codehaus.wadi.impl.StandardSessionWrapperFactory;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;
import org.codehaus.wadi.replication.contextualizer.ReplicaAwareContextualiser;
import org.codehaus.wadi.replication.manager.ReplicaterAdapterFactory;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.basic.BasicReplicationManagerFactory;
import org.codehaus.wadi.replication.manager.basic.DistributableManagerRehydrater;
import org.codehaus.wadi.replication.manager.basic.SessionReplicationManager;
import org.codehaus.wadi.replication.storage.basic.BasicReplicaStorageFactory;
import org.codehaus.wadi.replication.strategy.RoundRobinBackingStrategyFactory;
import org.codehaus.wadi.test.TestInvocation;
import org.codehaus.wadi.web.WebProxiedLocation;

/**
 * 
 * @version $Revision: 1603 $
 */
public abstract class AbstractReplicationContextualiserTest extends TestCase {
    private static final int MAX_LOOP = 10;
    private static final String CLUSTER_NAME = "CLUSTER";
    private static final long TIMEOUT = 5000L;
    
    private NodeInfo nodeInfo1;
    private NodeInfo nodeInfo2;
    
	protected void setUp() throws Exception {
        nodeInfo1 = setUpNode("node1");
        nodeInfo2 = setUpNode("node2");

        nodeInfo1.start();
        nodeInfo2.start();

        waitForStableCluster();
    }

	protected void tearDown() throws Exception {
	}

	public void testGetSessionFromReplicationManager() throws Exception {
		Session session = nodeInfo1.clusteredManager.create();
		String attrValue = "bar";
		String attrName = "foo";
        session.setAttribute(attrName, attrValue);
        ((RWLockListener) session).readEnded();
		String sessionId = session.getId();
        
        promoteNode2(sessionId);
        
        Session node2Session = (Session) nodeInfo2.mmap.get(sessionId);
        assertNotNull(node2Session);
        String actualAttrValue = (String) node2Session.getAttribute(attrName);
        assertEquals(attrValue, actualAttrValue);
        
        assertNull(nodeInfo1.mmap.get(attrName));
    }

    private void promoteNode2(String sessionId) throws InvocationException {
        nodeInfo2.clusteredManager.contextualise(
                new TestInvocation(null, null,
                    new FilterChain() { 
                        public void doFilter(ServletRequest req, ServletResponse res){} 
                    }), 
                    sessionId, null, null, false);
    }

    private void waitForStableCluster() throws InterruptedException {
        int nbLoop = 0;
        while (nodeInfo1.clusteredManager.getDispatcher().getNumNodes() < 2) {
            Thread.sleep(500);
            nbLoop++;
            if (nbLoop == MAX_LOOP) {
                fail();
            }
        }
        while (nodeInfo2.clusteredManager.getDispatcher().getNumNodes() < 2) {
            Thread.sleep(500);
            nbLoop++;
            if (nbLoop == MAX_LOOP) {
                fail();
            }
        }
    }

    protected abstract Dispatcher createDispatcher(String clusterName, String nodeName, long timeout) throws Exception;

    private NodeInfo setUpNode(String nodeName) throws Exception {
        Dispatcher dispatcher = createDispatcher(CLUSTER_NAME, nodeName, TIMEOUT); 
        
        int sweepInterval = 1000*60*60*24;
        Streamer streamer = new SimpleStreamer();
        Collapser collapser = new HashingCollapser(100, 1000);
        Map mmap = new HashMap();
        SimpleSessionPool sessionPool = new SimpleSessionPool(new AtomicallyReplicableSessionFactory());

        Evicter mevicter = new AlwaysEvicter(sweepInterval, true);
        ContextPool contextPool = new SessionToContextPoolAdapter(sessionPool);
        PoolableInvocationWrapperPool requestPool = new DummyStatefulHttpServletRequestWrapperPool();
        
        ReplicationManager replicationManager = createReplicationManager(dispatcher);
        
        DistributableManagerRehydrater sessionRehydrater =
            new DistributableManagerRehydrater();
        ReplicationManager sessionRepManager = 
            new SessionReplicationManager(replicationManager, sessionRehydrater);

        Contextualiser contextualiser = new DummyContextualiser();
        contextualiser = new ReplicaAwareContextualiser(contextualiser, sessionRepManager);
        contextualiser = new MemoryContextualiser(contextualiser, mevicter, mmap, streamer, contextPool, requestPool);

        ClusteredManager manager = new ClusteredManager(
                sessionPool, 
                new DistributableAttributesFactory(),
                new SimpleValuePool(new DistributableValueFactory()),
                new StandardSessionWrapperFactory(),
                new TomcatSessionIdFactory(),
                contextualiser,
                mmap,
                new DummyRouter(),
                true,
                streamer,
                true,
                new ReplicaterAdapterFactory(replicationManager),
                new WebProxiedLocation(new InetSocketAddress("localhost", 8080)),
                new StandardHttpProxy("jsessionid"),
                dispatcher,
                new DummyPartitionManager(72),
                collapser);

        sessionRehydrater.setManager(manager);
        
        manager.init(new DummyManagerConfig());
        
        return new NodeInfo(manager, replicationManager, mmap);
    }

    private ReplicationManager createReplicationManager(Dispatcher dispatcher) {
        BasicReplicationManagerFactory managerFactory = new BasicReplicationManagerFactory();
        ReplicationManager replicationManager = managerFactory.factory(dispatcher,
                new BasicReplicaStorageFactory(),
                new RoundRobinBackingStrategyFactory(1));
        return replicationManager;
    }
    
    private static class NodeInfo {
        private final ClusteredManager clusteredManager;
        private final ReplicationManager replicationManager;
        private final Map mmap;

        public NodeInfo(ClusteredManager clusteredManager, 
                ReplicationManager replicationManager,
                Map mmap) {
            this.clusteredManager = clusteredManager;
            this.replicationManager = replicationManager;
            this.mmap = mmap;
        }
        
        public void start() throws Exception {
            clusteredManager.start();
            replicationManager.start();
        }
        
        public void stop() throws Exception {
            replicationManager.stop();
            clusteredManager.stop();
        }
    }
}
