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

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.TestCase;

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.manager.ClusteredManager;
import org.codehaus.wadi.core.manager.Router;
import org.codehaus.wadi.core.session.DistributableAttributesFactory;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.core.session.ValueFactory;
import org.codehaus.wadi.core.session.ValueHelperRegistry;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.StackContext;
import org.codehaus.wadi.replication.manager.ReplicaterAdapterFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.test.MockInvocation;
import org.codehaus.wadi.test.MyHttpServletRequest;
import org.codehaus.wadi.web.BasicWebSessionFactory;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.impl.DummyRouter;
import org.codehaus.wadi.web.impl.StandardSessionWrapperFactory;

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
		Session session = nodeInfo1.clusteredManager.create(null);
		String attrValue = "bar";
		String attrName = "foo";
        session.addState(attrName, attrValue);
        session.onEndProcessing();
		String sessionName = session.getName();

        nodeInfo1.serviceSpace.stop();
        
        promoteNode(nodeInfo2, sessionName);
        
        WebSession node2Session = (WebSession) nodeInfo2.mmap.acquire(sessionName);
        assertNotNull(node2Session);
        String actualAttrValue = (String) node2Session.getAttribute(attrName);
        assertEquals(attrValue, actualAttrValue);
        
        assertNull(nodeInfo1.mmap.acquire(attrName));
    }

    private void promoteNode(NodeInfo nodeInfo, String sessionId) throws InvocationException {
        nodeInfo.clusteredManager.contextualise(
                new MockInvocation(new MyHttpServletRequest(sessionId), null,
                    new FilterChain() { 
                        public void doFilter(ServletRequest req, ServletResponse res){} 
                    }));
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
            protected Router newRouter() {
                return new DummyRouter();
            }
            
            protected SessionFactory newSessionFactory() {
                ValueHelperRegistry valueHelperRegistry = newValueHelperRegistry();
                ValueFactory valueFactory = newValueFactory(valueHelperRegistry);
                SimpleStreamer streamer = newStreamer();
                return new BasicWebSessionFactory(new DistributableAttributesFactory(valueFactory),
                        streamer,
                        new ReplicaterAdapterFactory(replicationManager),
                        router,
                        new StandardSessionWrapperFactory());
            }
        };
        stackContext.build();
        return new NodeInfo(stackContext.getServiceSpace(), stackContext.getManager(), stackContext.getMemoryMap());
    }

    private static class NodeInfo {
        private final ServiceSpace serviceSpace;
        private final ClusteredManager clusteredManager;
        private final ConcurrentMotableMap mmap;

        public NodeInfo(ServiceSpace serviceSpace, ClusteredManager clusteredManager, ConcurrentMotableMap mmap) {
            this.serviceSpace = serviceSpace;
            this.clusteredManager = clusteredManager;
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
