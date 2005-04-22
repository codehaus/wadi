/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.sandbox.test;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import junit.framework.TestCase;

import org.activecluster.ClusterException;
import org.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.HttpProxy;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Location;
import org.codehaus.wadi.sandbox.Motable;
import org.codehaus.wadi.sandbox.RelocationStrategy;
import org.codehaus.wadi.sandbox.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.impl.CustomCluster;
import org.codehaus.wadi.sandbox.impl.CustomClusterFactory;
import org.codehaus.wadi.sandbox.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.sandbox.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.impl.HttpProxyLocation;
import org.codehaus.wadi.sandbox.impl.MemoryContextualiser;
import org.codehaus.wadi.sandbox.impl.MessageDispatcher;
import org.codehaus.wadi.sandbox.impl.NeverEvicter;
import org.codehaus.wadi.sandbox.impl.StandardHttpProxy;
import org.codehaus.wadi.sandbox.impl.Utils;

/**
 * Test the shutdown of a Contextualiser stack as live sessions are distributed to other nodes in the cluster
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TestCluster extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());

    class MyNode {
        protected final CustomCluster _cluster;
        protected final MessageDispatcher _dispatcher;
        protected final Location _location;
        protected final RelocationStrategy _relocater;
        protected final Collapser _collapser=new HashingCollapser(10, 2000);
        protected final Map _cmap=new HashMap();
        protected final Map _mmap=new HashMap();
        protected final Evicter _evicter=new NeverEvicter();
        protected final MemoryContextualiser _top;
        protected final ClusterContextualiser _bottom;
        
        public MyNode(CustomClusterFactory factory, String clusterName) throws JMSException, ClusterException {
            _cluster=(CustomCluster)factory.createCluster(clusterName);
            _cluster.addClusterListener(new MyClusterListener());
            _dispatcher=new MessageDispatcher(_cluster);
            InetSocketAddress isa=new InetSocketAddress("localhost", 8080);
            HttpProxy proxy=new StandardHttpProxy("jsessionid");
            _location=new HttpProxyLocation(_cluster.getLocalNode().getDestination(), isa, proxy);
            //_relocater=new SwitchableRelocationStrategy();
            _relocater=null;
            _bottom=new ClusterContextualiser(new DummyContextualiser(), new NeverEvicter(), _cmap, _collapser, _cluster, _dispatcher, _relocater, _location);
            _top=new MemoryContextualiser(_bottom, _evicter, _mmap, new SimpleStreamingStrategy(), new MyContextPool(), new DummyStatefulHttpServletRequestWrapperPool());
            _bottom.setTop(_top);
        }
        
        protected boolean _running;
        
        public synchronized void start() throws Exception {
            if (!_running) {
                _cluster.start();
                _top.start();
                _running=true;
            }
        }
        
        public synchronized void stop() throws Exception {
            if (_running) {
                _log.info("stopping contextualiser stack...");
                _top.stop();
                _log.info("contextualiser stack stopped");
                _log.info("stopping cluster...");
                _cluster.stop();
                Thread.sleep(6000);
                _log.info("cluster stopped");
                _running=false;
            }
        }
        
        public Map getClusterContextualiserMap() {return _cmap;}
        public CustomCluster getCluster(){return _cluster;}
        public ClusterContextualiser getClusterContextualiser() {return _bottom;}
        
        public Map getMemoryContextualiserMap() {return _mmap;}
        public MemoryContextualiser getMemoryContextualiser(){return _top;}
    }

	protected ConnectionFactory _connectionFactory;
	protected CustomClusterFactory _clusterFactory;
	protected String _clusterName;
	protected MyNode _node0;
	protected MyNode _node1;
	protected MyNode _node2;

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
//        _connectionFactory=new ActiveMQConnectionFactory("peer://WADI-TEST");
        _connectionFactory=new ActiveMQConnectionFactory("tcp://localhost:61616");
        
		_clusterFactory=new CustomClusterFactory(_connectionFactory);
		_clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
		(_node0=new MyNode(_clusterFactory, _clusterName)).start();
		(_node1=new MyNode(_clusterFactory, _clusterName)).start();
		(_node2=new MyNode(_clusterFactory, _clusterName)).start();

		//_node0.getCluster().waitForClusterToComplete(3, 6000);
		//_node1.getCluster().waitForClusterToComplete(3, 6000);
		_node2.getCluster().waitForClusterToComplete(3, 6000);
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		Thread.sleep(1000);

		super.tearDown();
//		_node2.stop();
		Thread.sleep(6000);
		_node1.stop();
//		Thread.sleep(6000);
		_node0.stop();
//		Thread.sleep(10000);
	}

	/**
	 * Constructor for TestCluster.
	 * @param name
	 */
	public TestCluster(String name) {
		super(name);
	}

	public void testStop() throws Exception {
		Contextualiser c0=_node0.getMemoryContextualiser();
		Contextualiser c1=_node1.getMemoryContextualiser();
		Contextualiser c2=_node2.getMemoryContextualiser();

		Map m0=_node0.getMemoryContextualiserMap();
		Map m1=_node1.getMemoryContextualiserMap();
		Map m2=_node2.getMemoryContextualiserMap();
        

        // populate node0
		int numContexts=3;
		for (int i=0; i<numContexts; i++) {
			String id="session-"+i;
			Motable emotable=new MyContext(id, id);
			Immoter immoter=c0.getDemoter(id, emotable);
			Emoter emoter=new EtherEmoter();
			Utils.mote(emoter, immoter, emotable, id);
		}
		assertTrue(m0.size()==numContexts); // all sessions are on node0

        // shutdown node0
        // sessions should be evacuated to remaining two nodes...
        _log.info("NODES: "+_node0.getCluster().getNodes().size());
		_node0.stop();
        _log.info("NODES: "+_node1.getCluster().getNodes().size());

		// where are all the sessions now ?
		// the sum of nodes 1 and 2 should total n Contexts
		{
		    int s0=m0.size();
		    int s1=m1.size();
		    int s2=m2.size();
		    _log.info("dispersal - n0:"+s0+", n1:"+s1+", n2:"+s2);
		    assertTrue(s0==0);
		    assertTrue(s1+s2==numContexts);
            // TODO - hmmmm...
		    assertTrue(_node0.getClusterContextualiserMap().size()==numContexts); // node0 remembers where everything was sent...
		}

        // shutdown node1
        // sessions should all be evacuated to node2
        _log.info("NODES: "+_node1.getCluster().getNodes().size());
		_node1.stop();
        _log.info("NODES: "+_node2.getCluster().getNodes().size());
		{
		    int s0=m0.size();
		    int s1=m1.size();
		    int s2=m2.size();
		    _log.info("dispersal - n0:"+s0+", n1:"+s1+", n2:"+s2);
		    assertTrue(s0==0);
		    assertTrue(s1==0);
		    assertTrue(s2==numContexts);
            // TODO - hmmmm...
		    //assertTrue(_node0.getClusterContextualiserMap().size()==numContexts); // node0 remembers where everything was sent...
		}
		
        // shutdown node2
        _log.info("NODES: "+_node2.getCluster().getNodes().size());
		_node2.stop();
        _log.info("NODES: should be 0");
		Thread.sleep(6000); // give everything some time to happen...
		{
		    int s0=m0.size();
		    int s1=m1.size();
		    int s2=m2.size();
		    _log.info("dispersal - n0:"+s0+", n1:"+s1+", n2:"+s2);
		    assertTrue(s0==0);
		    assertTrue(s1==0);
		    assertTrue(s2==0);
		    //assertTrue(_node0.getClusterContextualiserMap().size()==numContexts); // node0 remembers where everything was sent...
		}

        // put something below node2 that will catch all the contexts, so we can restart...

        // TODO - figure out what should happen to location caches, implement and test it.
	}
}
