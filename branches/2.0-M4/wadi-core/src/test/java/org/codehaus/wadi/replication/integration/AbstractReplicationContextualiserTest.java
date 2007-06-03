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

import java.net.URI;

import junit.framework.TestCase;

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.contextualiser.BasicInvocationContextFactory;
import org.codehaus.wadi.core.contextualiser.InvocationContextFactory;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.contextualiser.ThrowExceptionIfNoSessionInvocation;
import org.codehaus.wadi.core.manager.DummyRouter;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.manager.Router;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

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
		Session session = nodeInfo1.manager.create(null);
		String attrValue = "bar";
		String attrName = "foo";
        session.addState(attrName, attrValue);
        session.onEndProcessing();
		String sessionName = session.getName();

        nodeInfo1.serviceSpace.stop();
        
        promoteNode(nodeInfo2, sessionName);
        
        Session node2Session = (Session) nodeInfo2.mmap.acquire(sessionName);
        assertNotNull(node2Session);
        String actualAttrValue = (String) node2Session.getState(attrName);
        assertEquals(attrValue, actualAttrValue);
        
        assertNull(nodeInfo1.mmap.acquire(attrName));
    }

    private void promoteNode(NodeInfo nodeInfo, String sessionId) throws InvocationException {
        nodeInfo.manager.contextualise(new ThrowExceptionIfNoSessionInvocation(sessionId));
    }

    private void waitForStableCluster() throws InterruptedException {
        int nbLoop = 0;
        while (nodeInfo1.serviceSpace.getDispatcher().getCluster().getPeerCount() < 2) {
            Thread.sleep(500);
            nbLoop++;
            if (nbLoop == MAX_LOOP) {
                fail();
            }
        }
        while (nodeInfo2.serviceSpace.getDispatcher().getCluster().getPeerCount() < 2) {
            Thread.sleep(500);
            nbLoop++;
            if (nbLoop == MAX_LOOP) {
                fail();
            }
        }
    }

    protected abstract void failNode(String nodeName);

    protected abstract Dispatcher createDispatcher(String clusterName, String nodeName, long timeout) throws Exception;

    private NodeInfo setUpNode(String nodeName) throws Exception {
        Dispatcher dispatcher = createDispatcher(CLUSTER_NAME, nodeName, TIMEOUT);
        dispatcher.start();
        
        StackContext stackContext = new StackContext(new ServiceSpaceName(new URI("name")), dispatcher) {
            protected InvocationContextFactory newInvocationContextFactory() {
                return new BasicInvocationContextFactory();
            }
            protected Router newRouter() {
                return new DummyRouter();
            }
        };
        stackContext.build();
        return new NodeInfo(stackContext.getServiceSpace(), stackContext.getManager(), stackContext.getMemoryMap());
    }

    private static class NodeInfo {
        private final ServiceSpace serviceSpace;
        private final Manager manager;
        private final ConcurrentMotableMap mmap;

        public NodeInfo(ServiceSpace serviceSpace, Manager manager, ConcurrentMotableMap mmap) {
            this.serviceSpace = serviceSpace;
            this.manager = manager;
            this.mmap = mmap;
        }
        
        public void start() throws Exception {
            serviceSpace.start();
        }
        
        public void stop() throws Exception {
            serviceSpace.stop();
        }
    }

}
