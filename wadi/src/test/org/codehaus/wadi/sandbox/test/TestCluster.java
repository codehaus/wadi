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
import javax.jms.Destination;
import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.ClusterException;
import org.codehaus.activemq.ActiveMQConnectionFactory;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.HttpProxy;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Location;
import org.codehaus.wadi.sandbox.Motable;
import org.codehaus.wadi.sandbox.RelocationStrategy;
import org.codehaus.wadi.sandbox.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.impl.HttpProxyLocation;
import org.codehaus.wadi.sandbox.impl.MemoryContextualiser;
import org.codehaus.wadi.sandbox.impl.MessageDispatcher;
import org.codehaus.wadi.sandbox.impl.NeverEvicter;
import org.codehaus.wadi.sandbox.impl.StandardHttpProxy;
import org.codehaus.wadi.sandbox.impl.SwitchableEvicter;
import org.codehaus.wadi.sandbox.impl.Utils;

import junit.framework.TestCase;

/**
 * Test the shutdown of a Contextualiser stack as live sessions are distributed to other nodes in the cluster
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TestCluster extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());

	class MyNode {
		protected final MyCluster _cluster;
		protected final MessageDispatcher _dispatcher;
		protected final Location _location;
		protected final RelocationStrategy _relocater;
		protected final Collapser _collapser=new HashingCollapser(10, 2000);
		protected final Map _cmap=new HashMap();
		protected final Map _mmap=new HashMap();
		protected final Evicter _evicter=new NeverEvicter();
		protected final MemoryContextualiser _top;
		protected final ClusterContextualiser _bottom;

		public MyNode(MyClusterFactory factory, String clusterName) throws JMSException, ClusterException {
			_cluster=(MyCluster)factory.createCluster(clusterName);
			_cluster.addClusterListener(new MyClusterListener());
			_dispatcher=new MessageDispatcher(_cluster);
			InetSocketAddress isa=new InetSocketAddress("localhost", 8080);
			HttpProxy proxy=new StandardHttpProxy("jsessionid");
			_location=new HttpProxyLocation(_cluster.getLocalNode().getDestination(), isa, proxy);
			//_relocater=new SwitchableRelocationStrategy();
			_relocater=null;
			_bottom=new ClusterContextualiser(new DummyContextualiser(), new SwitchableEvicter(), _cmap, _collapser, _dispatcher, _relocater, _location);
			_top=new MemoryContextualiser(_bottom, _evicter, _mmap, new SimpleStreamingStrategy(), new MyContextPool());
			_bottom.setTop(_top);
		}

		public void start() throws Exception {_cluster.start();}
		public void stop() throws Exception {_cluster.stop();}

		public Map getClusterContextualiserMap() {return _cmap;}
		public MyCluster getCluster(){return _cluster;}
		public ClusterContextualiser getClusterContextualiser() {return _bottom;}

		public Map getMemoryContextualiserMap() {return _mmap;}
		public MemoryContextualiser getMemoryContextualiser(){return _top;}
	}

	protected ConnectionFactory _connectionFactory;
	protected MyClusterFactory _clusterFactory;
	protected String _clusterName;
	protected MyNode _node0;
	protected MyNode _node1;
	protected MyNode _node2;

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		_connectionFactory=new ActiveMQConnectionFactory("peer://WADI-TEST");
		_clusterFactory=new MyClusterFactory(_connectionFactory);
		_clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
		(_node0=new MyNode(_clusterFactory, _clusterName)).start();
		(_node1=new MyNode(_clusterFactory, _clusterName)).start();
		(_node2=new MyNode(_clusterFactory, _clusterName)).start();

		_node0.getCluster().waitForClusterToComplete(3, 6000);
		_node1.getCluster().waitForClusterToComplete(3, 6000);
		_node2.getCluster().waitForClusterToComplete(3, 6000);
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		Thread.sleep(1000);

		super.tearDown();
		_node2.stop();
		Thread.sleep(6000);
		_node1.stop();
		Thread.sleep(6000);
		_node0.stop();
		Thread.sleep(10000);
	}

	/**
	 * Constructor for TestCluster.
	 * @param name
	 */
	public TestCluster(String name) {
		super(name);
	}

	public void testDemotion() throws Exception {
		assertTrue(true);

		ClusterContextualiser c=_node0.getClusterContextualiser();
		Destination queue=_node0.getCluster().createQueue("EMIGRATION");

		int numContexts=100;
		c.setEmigrationQueue(queue);
		for (int i=0; i<numContexts; i++) {
			String id="session-"+i;
			Motable emotable=new MyContext(id, id);
			Immoter immoter=c.getDemoter(id, emotable);
			Emoter emoter=new EtherEmoter();
			Utils.mote(emoter, immoter, emotable, id);
		}
		// demote n Contexts into node0
		// they should be distributed to nodes 1 and 2
		// node0 should have 0 Contexts
		// the sum of nodes 1 and 2 should total n Contexts
		int s1=_node1.getMemoryContextualiserMap().size();
		int s2=_node2.getMemoryContextualiserMap().size();
		_log.info("dispersal - n1:"+s1+", n2:"+s2);
		assertTrue(s1+s2==100);

		assertTrue(numContexts==_node0.getClusterContextualiserMap().size());
		_log.info("new locations: "+_node0.getClusterContextualiserMap());

		// as they shut down more exciting stuff should happen...
	}
}
