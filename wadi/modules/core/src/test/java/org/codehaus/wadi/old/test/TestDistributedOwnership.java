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

// this test should create a lot of threads, each representing a node.

// each node will own a set of 'sessions'

// the threads will continually negotiate new ownership of the sessions and migrate them between each other, until i am satisfied that I have a bulletproof strategy...

package org.codehaus.wadi.old.test;

import javax.jms.JMSException;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.activecluster.Cluster;
import org.apache.activecluster.ClusterException;
import org.apache.activecluster.ClusterFactory;
import org.apache.activecluster.impl.DefaultClusterFactory;
import org.codehaus.wadi.impl.Utils;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 *
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class TestDistributedOwnership extends TestCase {

	public TestDistributedOwnership(String name) {
		super(name);
	}

	protected Log _log = LogFactory.getLog(TestCluster.class);
	protected ActiveMQConnectionFactory _connectionFactory;
	protected DefaultClusterFactory _clusterFactory;

	class Node extends Thread {
		protected String _name;
		protected ClusterFactory _factory;
		protected String  _id;
		protected Cluster _cluster;
		protected Log     _log;

		Node(ClusterFactory factory, String name, String id)
		throws ClusterException, JMSException
		{
			_factory = factory;
			_name = name;
			_cluster = _clusterFactory.createCluster(_name);
			_id      = id;
			_log     = LogFactory.getLog(getClass().getName()+"#"+_id);
		}

		public void run() {
			try {
				_cluster.start();
			} catch (JMSException e) {
			  _log.error("could not start node", e);
			}

			try
			{
			  _log.info("running...");
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			  _log.error("interrupted", e);
		}
			try {
				_cluster.stop();
			} catch (JMSException e) {
			  _log.error("could not stop node", e);
			}
			_cluster = null;
		}

	}

	protected int    _numNodes=100;
	protected Node[] _nodes=new Node[_numNodes];

	protected void setUp() throws Exception {
		_connectionFactory = Utils.getConnectionFactory();
		_clusterFactory = new DefaultClusterFactory(_connectionFactory);

		for (int i=0; i<_numNodes; i++)
			_nodes[i]=new Node(_clusterFactory, "ORG.CODEHAUS.WADI.TEST.CLUSTER", ""+i);
	}

	protected void tearDown() throws JMSException {
		_nodes=null;
	}

	protected void
	startNodes()
	{
		for (int i=0; i<_numNodes; i++)
			_nodes[i].start();
	}

	protected void
	stopNodes()
	{
//	 for (int i=0; i<_numNodes; i++)
//	 	_nodes[i].stop();

	 for (int i=0; i<_numNodes; i++)
	 {
	 	try
		{
	 		_nodes[i].join();
		}
	 	catch (InterruptedException e)
		{
		  _log.warn("interrupted whilst stopping thread", e);
		}
	 }
	}

	public void
	testThreads()
	{
		startNodes();
		stopNodes();
	}
}
