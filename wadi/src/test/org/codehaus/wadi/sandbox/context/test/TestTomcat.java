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

import java.net.InetSocketAddress;

import javax.jms.ConnectionFactory;

import org.codehaus.activecluster.ClusterFactory;
import org.codehaus.activecluster.impl.DefaultClusterFactory;
import org.codehaus.activemq.ActiveMQConnectionFactory;

import junit.framework.TestCase;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

// DOH! - looks like you can't run more than one TC per JVM... - investigate later...

public class TestTomcat extends TestCase {

	protected Node _node0;
	protected Node _node1;
	protected MyFilter _filter0;
	protected MyFilter _filter1;
	protected MyServlet _servlet0;
	protected MyServlet _servlet1;
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://WADI-TEST");
		ClusterFactory clusterFactory       = new DefaultClusterFactory(connectionFactory);
		String clusterName                  = "ORG.CODEHAUS.WADI.TEST.CLUSTER";
		_servlet0=new MyServlet("0", clusterFactory.createCluster(clusterName), new InetSocketAddress("localhost", 8080), new MyContextPool());
		_filter0=new MyFilter("0", _servlet0);
		(_node0=new TomcatNode("0", "localhost", 8080, "/test", "/*", _filter0, _servlet0)).start();
//		_servlet1=new MyServlet("1", clusterFactory.createCluster(clusterName), new InetSocketAddress("localhost", 8081), new MyContextPool());
//		_filter1=new MyFilter("1", _servlet1);
//		(_node1=new TomcatNode("1", "localhost", 8081, "/test", "/*", _filter1, _servlet1)).start();
	    }

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		_node0.stop();
//		_node1.stop();
	}

	/**
	 * Constructor for TestTomcat.
	 * @param name
	 */
	public TestTomcat(String name) {
		super(name);
	}
	
	public void testTomcat() throws Exception {
		assertTrue(true);
//	    Thread.sleep(30000);
	}
}
