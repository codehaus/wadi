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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import javax.jms.ConnectionFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.ClusterFactory;
import org.codehaus.activecluster.impl.DefaultClusterFactory;
import org.codehaus.activemq.ActiveMQConnectionFactory;

import junit.framework.TestCase;

/**
 * Unit Tests requiring a pair of Jetty's. Each one is set up with a Filter and Servlet placeholder.
 * These are injected with actual Filter and Servlet instances before the running of each test. This
 * allows the tests to set up the innards of these components, make http requests to them and then inspect
 * their innards for the expected changes,
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TestMigration extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());
	
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
		System.setProperty("org.mortbay.xml.XmlParser.NotValidating", "true");
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://WADI-TEST");
		ClusterFactory clusterFactory       = new DefaultClusterFactory(connectionFactory);
		String clusterName                  = "ORG.CODEHAUS.WADI.TEST.CLUSTER";
		_servlet0=new MyServlet("0", clusterFactory.createCluster(clusterName), new InetSocketAddress("localhost", 8080), new MyContextPool());
		_filter0=new MyFilter("0", _servlet0);
		(_node0=new JettyNode("0", "localhost", 8080, "/test", "/*", _filter0, _servlet0)).start();
		_servlet1=new MyServlet("1", clusterFactory.createCluster(clusterName), new InetSocketAddress("localhost", 8081), new MyContextPool());
		_filter1=new MyFilter("1", _servlet1);
		(_node1=new JettyNode("1", "localhost", 8081, "/test", "/*", _filter1, _servlet1)).start();
	}
	
	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		_node1.stop();
		_node0.stop();
		super.tearDown();
	}

	/**
	 * Constructor for TestMigration.
	 * @param name
	 */
	public TestMigration(String name) {
		super(name);
	}

	public int get(HttpClient client, HttpMethod method, String path) throws IOException, HttpException {
		method.recycle();
		method.setPath(path);
		client.executeMethod(method);
		return method.getStatusCode();
	}
	
	public void testMigration() throws Exception {
	    Thread.sleep(2000); // activecluster needs a little time to sort itself out...
	    _log.info("STARTING NOW!");

	    HttpClient client=new HttpClient();
		HttpMethod method0=new GetMethod("http://localhost:8080");
		HttpMethod method1=new GetMethod("http://localhost:8081");

		Map m0=_servlet0.getMemoryMap();
		Map m1=_servlet1.getMemoryMap();
		Map c0=_servlet0.getClusterMap();
		Map c1=_servlet1.getClusterMap();
		
		assertTrue(m0.isEmpty());
		assertTrue(m1.isEmpty());
		assertTrue(c0.isEmpty());
		assertTrue(c1.isEmpty());
		
		// no sessions available locally
		_filter0.setLocalOnly(true);
		assertTrue(get(client, method0, "/test;jsessionid=foo")!=200);
		assertTrue(get(client, method0, "/test;jsessionid=bar")!=200);
		_filter1.setLocalOnly(true);
		assertTrue(get(client, method1, "/test;jsessionid=foo")!=200);
		assertTrue(get(client, method1, "/test;jsessionid=bar")!=200);
		
		assertTrue(m0.isEmpty());
		assertTrue(m1.isEmpty());
		assertTrue(c0.isEmpty());
		assertTrue(c1.isEmpty());
		
		m0.put("foo", new MyContext());
		m1.put("bar", new MyContext());

		assertTrue(m0.size()==1);
		assertTrue(m1.size()==1);
		assertTrue(c0.isEmpty());
		assertTrue(c1.isEmpty());
		
		// 2/4 sessions available locally
		_filter0.setLocalOnly(true);
		assertTrue(get(client, method0, "/test;jsessionid=foo")==200);
		assertTrue(get(client, method0, "/test;jsessionid=bar")!=200);
		_filter1.setLocalOnly(true);
		assertTrue(get(client, method1, "/test;jsessionid=foo")!=200);
		assertTrue(get(client, method1, "/test;jsessionid=bar")==200);

		assertTrue(m0.size()==1);
		assertTrue(m1.size()==1);
		assertTrue(c0.isEmpty());
		assertTrue(c1.isEmpty());
		
		// 4/4 sessions available locally|remotely
		_filter0.setLocalOnly(false);
		assertTrue(get(client, method0, "/test;jsessionid=foo")==200);
		assertTrue(get(client, method0, "/test;jsessionid=bar")==200);
		_filter1.setLocalOnly(false);
		assertTrue(get(client, method1, "/test;jsessionid=foo")==200);
		assertTrue(get(client, method1, "/test;jsessionid=bar")==200);

		assertTrue(m0.size()==1);
		assertTrue(m1.size()==1);
		assertTrue(c0.size()==1); // location from clusterwide query has been cached
		assertTrue(c1.size()==1); // location from clusterwide query has been cached
		
		// ensure that cached locations work second time around...
		_filter0.setLocalOnly(false);
		assertTrue(get(client, method0, "/test;jsessionid=foo")==200);
		assertTrue(get(client, method0, "/test;jsessionid=bar")==200);
		_filter1.setLocalOnly(false);
		assertTrue(get(client, method1, "/test;jsessionid=foo")==200);
		assertTrue(get(client, method1, "/test;jsessionid=bar")==200);
		
		assertTrue(m0.size()==1);
		assertTrue(m1.size()==1);
		assertTrue(c0.size()==1);
		assertTrue(c1.size()==1);
		
		c0.clear();
		c1.clear();
		// won't run locally
		assertTrue(get(client, method0, "/test/confidential;jsessionid=foo")==403); // forbidden
		// won't run remotely
		assertTrue(get(client, method0, "/test/confidential;jsessionid=bar")==403); // forbidden
		_node0.setSecure(true);
		
		// will run locally - since we have declared the Listener secure
		assertTrue(get(client, method0, "/test/confidential;jsessionid=foo")==200);
		// will run remotely - proxy should preserve confidentiality on remote server...
		assertTrue(get(client, method0, "/test/confidential;jsessionid=bar")==200);

		// next test should be that we can somehow migrate sessions across, in place of proxying...
		
		// TODO:
		// consider merging two classes
		// consider having MigrationConceptualiser at top of stack (to promote sessions to other nodes)
		// Consider a MigrationContextualiser in place of the Location tier, which only migrates
		// and a HybridContextualiser, which sometimes proxies and sometimes migrates...
		// consider moving back to a more jcache like architecture, where the CacheKey is a compound - Req, Chain, etc...
		// lookup returns a FilterChain to be run.... (problem - pause between lookup and locking - think about it).
		// if we have located a session and set up a timeout, this should be released after the first proxy to it...
		// 8080, 8081 should only be encoded once...

	    _log.info("STOPPING NOW!");
	    Thread.sleep(2000); // activecluster needs a little time to sort itself out...
	    }	
}
