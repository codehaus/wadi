/**
*
* Copyright 2003-2004 The Apache Software Foundation
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
package org.codehaus.wadi.sandbox.context.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;
import org.codehaus.activecluster.ClusterException;
import org.codehaus.activecluster.LocalNode;
import org.codehaus.activecluster.impl.DefaultCluster;
import org.codehaus.activecluster.impl.DefaultClusterFactory;
import org.codehaus.activecluster.impl.ReplicatedLocalNode;
import org.codehaus.activecluster.impl.StateService;
import org.codehaus.activecluster.impl.StateServiceStub;
import org.codehaus.activemq.ActiveMQConnectionFactory;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Location;
import org.codehaus.wadi.sandbox.context.RelocationStrategy;
import org.codehaus.wadi.sandbox.context.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.context.impl.DummyCollapser;
import org.codehaus.wadi.sandbox.context.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.context.impl.MessageDispatcher;
import org.codehaus.wadi.sandbox.context.impl.NeverEvicter;

import junit.framework.TestCase;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TestCluster extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());

	class MyCluster extends DefaultCluster {
	    public MyCluster(final LocalNode localNode, Topic dataTopic, Topic destination, Connection connection, Session session, MessageProducer producer, Timer timer, long inactiveTime) throws JMSException {
	    	super(localNode, dataTopic, destination, connection, session, producer, timer, inactiveTime);
	    }
	    
	    public Destination createQueue(String name) throws JMSException {
	    	return getSession().createQueue(name);
	    }
	}
	
	class MyClusterFactory extends DefaultClusterFactory {
		public MyClusterFactory(ConnectionFactory connectionFactory) {
			super(connectionFactory);
		}
		
	    protected Cluster createCluster(Connection connection, Session session, Topic groupDestination) throws JMSException {
	        Topic dataTopic=session.createTopic(getDataTopicPrefix()+groupDestination.getTopicName());
	        MessageProducer producer=createProducer(session, null);
	        producer.setDeliveryMode(getDeliveryMode());
	        MessageProducer keepAliveProducer=session.createProducer(dataTopic);
	        keepAliveProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
	        StateService serviceStub=new StateServiceStub(session, keepAliveProducer);
	        Destination localInbox=null;
	        if (isUseQueueForInbox()) {
	            localInbox=session.createTemporaryQueue();
	        } else {
	            localInbox=session.createTemporaryTopic();
	        }
	        ReplicatedLocalNode localNode=new ReplicatedLocalNode(localInbox, serviceStub);
	        Timer timer=new Timer();
	        MyCluster answer=new MyCluster(localNode, dataTopic, groupDestination, connection, session, producer, timer, getInactiveTime());
	        return answer;
	    }
	}
	
	class MyNode {
		protected final MyCluster _cluster;	
		protected final MessageDispatcher _dispatcher;
		protected final Location _location;
		protected final RelocationStrategy _relocater;
		protected final Collapser _collapser=new DummyCollapser();
		protected final Map _map=new HashMap();
		protected final Evicter _evicter=new NeverEvicter();
		protected final ClusterContextualiser _contextualiser;
		
		public MyNode(MyClusterFactory factory, String clusterName) throws JMSException, ClusterException {
			_cluster=(MyCluster)factory.createCluster(clusterName);
			_cluster.addClusterListener(new MyClusterListener());
			_dispatcher=new MessageDispatcher(_cluster);
//			InetSocketAddress isa0=new InetSocketAddress("localhost", 8080);
//			HttpProxy proxy0=new StandardHttpProxy("jsessionid");
//			_location0=new HttpProxyLocation(_cluster0.getLocalNode().getDestination(), isa0, proxy0);
//			_relocater0=new SwitchableRelocationStrategy();
			_location=null;
			_relocater=null;
			_contextualiser=new ClusterContextualiser(new DummyContextualiser(), _collapser, _map, _evicter, _dispatcher, _relocater);
		}
		
		public void start() throws Exception {_cluster.start();}
		public void stop() throws Exception {_cluster.stop();}
		
		public Map getMap() {return _map;}
		public MyCluster getCluster(){return _cluster;}
		public ClusterContextualiser getContextualiser() {return _contextualiser;}
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
		
		ClusterContextualiser c=_node0.getContextualiser();
		Destination queue=_node0.getCluster().createQueue("EMMIGRATION");
		
		c.setEmmigrationQueue(queue);
		for (int i=0; i<100; i++) {
			_log.info("*** "+i+" ***");
			c.demote("foo", new DummyMotable());
			c.demote("bar", new DummyMotable());
			c.demote("baz", new DummyMotable());
		}
		
		// demote n Contexts into node0
		// they should be distributed to nodes 1 and 2
		// node0 should have 0 Contexts
		// the sum of nodes 1 and 2 should total n Contexts
		
		// as they shut down more exciting stuff should happen...
	}
}
