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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.activecluster.ClusterFactory;
import org.activemq.ActiveMQConnectionFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.HttpProxy;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Location;
import org.codehaus.wadi.sandbox.RelocationStrategy;
import org.codehaus.wadi.sandbox.impl.CommonsHttpProxy;
import org.codehaus.wadi.sandbox.impl.CustomCluster;
import org.codehaus.wadi.sandbox.impl.CustomClusterFactory;
import org.codehaus.wadi.sandbox.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.impl.HttpProxyLocation;
import org.codehaus.wadi.sandbox.impl.ImmigrateRelocationStrategy;
import org.codehaus.wadi.sandbox.impl.MessageDispatcher;
import org.codehaus.wadi.sandbox.impl.ProxyRelocationStrategy;
import org.codehaus.wadi.sandbox.impl.StandardHttpProxy;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Unit Tests requiring a pair of Jetty's. Each one is set up with a Filter and Servlet placeholder.
 * These are injected with actual Filter and Servlet instances before the running of each test. This
 * allows the tests to set up the innards of these components, make http requests to them and then inspect
 * their innards for the expected changes,
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TestRelocation extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());

	protected Node _node0;
	protected Node _node1;
	protected MyFilter _filter0;
	protected MyFilter _filter1;
	protected MyServlet _servlet0;
	protected MyServlet _servlet1;
	protected CustomCluster _cluster0;
	protected CustomCluster _cluster1;
	protected Location _location0;
	protected Location _location1;
	protected MessageDispatcher _dispatcher0;
	protected MessageDispatcher _dispatcher1;
	protected SwitchableRelocationStrategy _relocater0;
	protected SwitchableRelocationStrategy _relocater1;

	  class SwitchableRelocationStrategy implements RelocationStrategy {
	  	protected RelocationStrategy _delegate=new DummyRelocationStrategy();

	  	public void setRelocationStrategy(RelocationStrategy delegate){
	  		delegate.setTop(_delegate.getTop());
	  		_delegate=delegate;
	  	}

	  	// RelocationStrategy

		public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock, Map locationMap) throws IOException, ServletException {
			return _delegate.relocate(hreq, hres, chain, id, immoter, promotionLock, locationMap);
		}

		public void setTop(Contextualiser top) {
			_delegate.setTop(top);
		}

		public Contextualiser getTop(){return _delegate.getTop();}
	}

	  class DummyRelocationStrategy implements RelocationStrategy {
		public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock, Map locationMap) {return false;}
		protected Contextualiser _top;
		public void setTop(Contextualiser top) {_top=top;}
		public Contextualiser getTop(){return _top;}
	}

	  /*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
//        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://WADI-TEST");
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
//		ClusterFactory clusterFactory       = new DefaultClusterFactory(connectionFactory,false, Session.AUTO_ACKNOWLEDGE, "ACTIVECLUSTER.DATA.", 50000L);
		ClusterFactory clusterFactory       = new CustomClusterFactory(connectionFactory);
		String clusterName                  = "ORG.CODEHAUS.WADI.TEST.CLUSTER";

		InetSocketAddress isa0=new InetSocketAddress("localhost", 8080);
		_cluster0=(CustomCluster)clusterFactory.createCluster(clusterName);
		_cluster0.addClusterListener(new MyClusterListener());
		HttpProxy proxy0=new StandardHttpProxy("jsessionid");
		_dispatcher0=new MessageDispatcher(_cluster0);
		_location0=new HttpProxyLocation(_cluster0.getLocalNode().getDestination(), isa0, proxy0);
		_relocater0=new SwitchableRelocationStrategy();
		_servlet0=new MyServlet("0", _cluster0, new MyContextPool(), _dispatcher0, _relocater0, _location0);
		_filter0=new MyFilter("0", _servlet0);
		// TODO - I'd like to use a TomcatNode - but using 5.0.18 it fails TestRelocation - investigate...
		(_node0=new JettyNode("0", "localhost", 8080, "/test", "/home/jules/workspace/wadi/webapps/test", _filter0, _servlet0)).start();

		InetSocketAddress isa1=new InetSocketAddress("localhost", 8081);
		_cluster1=(CustomCluster)clusterFactory.createCluster(clusterName);
		_cluster1.addClusterListener(new MyClusterListener());
		HttpProxy proxy1=new CommonsHttpProxy("jsessionid");
		_dispatcher1=new MessageDispatcher(_cluster1);
		_location1=new HttpProxyLocation(_cluster1.getLocalNode().getDestination(), isa1, proxy1);
		_relocater1=new SwitchableRelocationStrategy();
		_servlet1=new MyServlet("1", _cluster1, new MyContextPool(), _dispatcher1, _relocater1, _location1);
		_filter1=new MyFilter("1", _servlet1);
		(_node1=new JettyNode("1", "localhost", 8081, "/test", "/home/jules/workspace/wadi/webapps/test", _filter1, _servlet1)).start();
	    Thread.sleep(2000); // activecluster needs a little time to sort itself out...
	    _log.info("STARTING NOW!");
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
	    _log.info("STOPPING NOW!");
	    Thread.sleep(2000); // activecluster needs a little time to sort itself out...

	    _node1.stop();
		_node0.stop();
		super.tearDown();
	}

	/**
	 * Constructor for TestMigration.
	 * @param name
	 */
	public TestRelocation(String name) {
		super(name);
	}

	public int get(HttpClient client, HttpMethod method, String path) throws IOException, HttpException {
		client.setState(new HttpState());
		method.recycle();
		method.setPath(path);
		client.executeMethod(method);
		return method.getStatusCode();
	}

	public void testProxyInsecureRelocation() throws Exception {
		_relocater0.setRelocationStrategy(new ProxyRelocationStrategy(_dispatcher0, _location0, 2000, 3000));
		_relocater1.setRelocationStrategy(new ProxyRelocationStrategy(_dispatcher1, _location1, 2000, 3000));
		testInsecureRelocation(false);
		}

	public void testMigrateInsecureRelocation() throws Exception {
        Collapser collapser=new HashingCollapser(10, 2000);
		_relocater0.setRelocationStrategy(new ImmigrateRelocationStrategy(_dispatcher0, _location0, 2000, _servlet0.getClusterMap(), collapser));
		_relocater1.setRelocationStrategy(new ImmigrateRelocationStrategy(_dispatcher1, _location1, 2000, _servlet1.getClusterMap(), collapser));
		testInsecureRelocation(true);
		}

	public void testInsecureRelocation(boolean migrating) throws Exception {
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
		int status=get(client, method0, "/test;jsessionid=foo");
		_log.info("STATUS="+status);
		assertTrue(status==200);
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

		if (migrating) {
			assertTrue(m0.size()==2);
			Thread.sleep(1000); // can take a while for ACK to be processed
			assertTrue(m1.size()==0);
			assertTrue(c0.size()==0); // n0 has all the sessions, so needn't remember any further locations...
			assertTrue(c1.size()==1); // bar migrated from n1 to n0, so n1 needs to remember the new location...
		} else {
			assertTrue(m0.size()==1);
			assertTrue(m1.size()==1);
			assertTrue(c0.size()==1); // n0 had to proxy a request to n1, so needs to remember the location
			assertTrue(c1.size()==0);
		}

		_filter1.setLocalOnly(false);
		assertTrue(get(client, method1, "/test;jsessionid=foo")==200);
		assertTrue(get(client, method1, "/test;jsessionid=bar")==200);

		if (migrating) {
			assertTrue(m1.size()==2);
			Thread.sleep(1000); // can take a while for ACK to be processed
			assertTrue(m0.size()==0);
			assertTrue(c0.size()==2); // n0 should now know that both sessions are on n1
			_log.info("M0="+m0);
			_log.info("M1="+m1);
			_log.info("C0="+c0);
			_log.info("C1="+c1);
			assertTrue(c1.size()==0); // n1 has all the sessions and doesn't need to know anything...
		} else {
			assertTrue(m0.size()==1);
			assertTrue(m1.size()==1);
			assertTrue(c0.size()==1); // location from clusterwide query has been cached
			assertTrue(c1.size()==1); // location from clusterwide query has been cached
		}

		// ensure that cached locations work second time around...
		_filter0.setLocalOnly(false);
		assertTrue(get(client, method0, "/test;jsessionid=foo")==200);
		assertTrue(get(client, method0, "/test;jsessionid=bar")==200);

		if (migrating) {
			assertTrue(m0.size()==2);
			Thread.sleep(1000); // can take a while for ACK to be processed
			assertTrue(m1.size()==0);
			assertTrue(c0.size()==0); // n1 had all the sessions, now n0 has
			assertTrue(c1.size()==2); // n1 needs to know all their new locations
		} else {
			assertTrue(m0.size()==1);
			assertTrue(m1.size()==1);
			assertTrue(c0.size()==1); // no change - everyone already knows
			assertTrue(c1.size()==1); // all locations...
		}

		_filter1.setLocalOnly(false);
		assertTrue(get(client, method1, "/test;jsessionid=foo")==200);
		assertTrue(get(client, method1, "/test;jsessionid=bar")==200);

		if (migrating) {
			assertTrue(m1.size()==2);
			Thread.sleep(1000); // can take a while for ACK to be processed
			assertTrue(m0.size()==0);
			assertTrue(c0.size()==2); // n0 needs to know all their new locations
			assertTrue(c1.size()==0); // n0 had all the sessions, now n1 has
		} else {
			assertTrue(m0.size()==1);
			assertTrue(m1.size()==1);
			assertTrue(c0.size()==1);
			assertTrue(c1.size()==1);
		}
	}

	public void testProxySecureRelocation() throws Exception {
		_relocater0.setRelocationStrategy(new ProxyRelocationStrategy(_dispatcher0, _location0, 2000, 3000));
		_relocater1.setRelocationStrategy(new ProxyRelocationStrategy(_dispatcher1, _location1, 2000, 3000));
		testSecureRelocation(false);
		}

	public void testMigrateSecureRelocation() throws Exception {
        Collapser collapser=new HashingCollapser(10, 2000);
		_relocater0.setRelocationStrategy(new ImmigrateRelocationStrategy(_dispatcher0, _location0, 2000, _servlet0.getClusterMap(), collapser));
		_relocater1.setRelocationStrategy(new ImmigrateRelocationStrategy(_dispatcher1, _location1, 2000, _servlet1.getClusterMap(), collapser));
		testSecureRelocation(true);
		}

	public void testSecureRelocation(boolean migrating) throws Exception {
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

		m0.put("foo", new MyContext());
		m1.put("bar", new MyContext());

		assertTrue(m0.size()==1);
		assertTrue(m1.size()==1);
		assertTrue(c0.isEmpty());
		assertTrue(c1.isEmpty());

		assertTrue(!_node0.getSecure());
		// won't run locally
		assertTrue(get(client, method0, "/test/confidential;jsessionid=foo")==403); // forbidden
		// won't run remotely
		assertTrue(get(client, method0, "/test/confidential;jsessionid=bar")==403); // forbidden

		_node0.setSecure(true);
		assertTrue(_node0.getSecure());
		// will run locally - since we have declared the Listener secure
		assertTrue(get(client, method0, "/test/confidential;jsessionid=foo")==200);
		// will run remotely - proxy should preserve confidentiality on remote server...
		assertTrue(get(client, method0, "/test/confidential;jsessionid=bar")==200);

		assertTrue(!_node1.getSecure());
		// won't run locally
		assertTrue(get(client, method1, "/test/confidential;jsessionid=bar")==403); // forbidden
		// won't run remotely
		assertTrue(get(client, method1, "/test/confidential;jsessionid=foo")==403); // forbidden

		_node1.setSecure(true);
		assertTrue(_node1.getSecure());
		// will run locally - since we have declared the Listener secure
		assertTrue(get(client, method1, "/test/confidential;jsessionid=bar")==200);
		// will run remotely - proxy should preserve confidentiality on remote server...
		assertTrue(get(client, method1, "/test/confidential;jsessionid=foo")==200);
	}

	// TODO:
	// if we have located a session and set up a timeout, this should be released after the first proxy to it...
	// 8080, 8081 should only be encoded once...

	static class Test implements MyServlet.Test {
		protected int _count=0;
		public int getCount(){return _count;}
		public void setCount(int count){_count=count;}

		protected boolean _stateful;
		public boolean isStateful(){return _stateful;}

		public void test(ServletRequest req, ServletResponse res){
			_count++;
			try {
				((javax.servlet.http.HttpServletRequest)req).getSession();
				_stateful=true;
			} catch (UnsupportedOperationException ignore){
				_stateful=false;
			}
		}
	}

	public void testRelocationStatelessContextualiser() throws Exception {
		_relocater0.setRelocationStrategy(new ProxyRelocationStrategy(_dispatcher0, _location0, 2000, 3000));
		_relocater1.setRelocationStrategy(new ProxyRelocationStrategy(_dispatcher1, _location1, 2000, 3000));
		testStatelessContextualiser(false);
		}

	public void testMigrateStatelessContextualiser() throws Exception {
        Collapser collapser=new HashingCollapser(10, 2000);
		_relocater0.setRelocationStrategy(new ImmigrateRelocationStrategy(_dispatcher0, _location0, 2000, _servlet0.getClusterMap(), collapser));
		_relocater1.setRelocationStrategy(new ImmigrateRelocationStrategy(_dispatcher1, _location1, 2000, _servlet1.getClusterMap(), collapser));
		testStatelessContextualiser(true);
		}

	public void testStatelessContextualiser(boolean migrating) throws Exception {

		HttpClient client=new HttpClient();
		HttpMethod method0=new GetMethod("http://localhost:8080");
		HttpMethod method1=new GetMethod("http://localhost:8081");

		Map m0=_servlet0.getMemoryMap();
		Map m1=_servlet1.getMemoryMap();

		m0.put("foo", new MyContext());
		m1.put("bar", new MyContext());

		Test test;

		// this won't be proxied, because we can prove that it is stateless...
		test=new Test();
		_servlet0.setTest(test);
		assertTrue(get(client, method0, "/test/static.html;jsessionid=bar")==200);
		assertTrue(test.getCount()==1 && !test.isStateful());
		_servlet0.setTest(null);

		MyServlet servlet;

		servlet=migrating?_servlet0:_servlet1;
		// this will be proxied, because we cannot prove that it is stateless...
		test=new Test();
		servlet.setTest(test);
		assertTrue(get(client, method0, "/test/dynamic.dyn;jsessionid=bar")==200);
		assertTrue(test.getCount()==1 && test.isStateful());
		servlet.setTest(null);

		// this won't be proxied, because we can prove that it is stateless...
		test=new Test();
		_servlet1.setTest(test);
		assertTrue(get(client, method1, "/test/static.html;jsessionid=foo")==200);
		assertTrue(test.getCount()==1 && !test.isStateful());
		_servlet1.setTest(null);

		servlet=migrating?_servlet1:_servlet0;
		// this will be proxied, because we cannot prove that it is stateless...
		test=new Test();
		servlet.setTest(test);
		assertTrue(get(client, method1, "/test/dynamic.jsp;jsessionid=foo")==200);
		assertTrue(test.getCount()==1 && test.isStateful());
		servlet.setTest(null);
	}
}
