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
package org.codehaus.wadi.jetty5;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import junit.framework.TestCase;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.Relocater;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.AbstractRelocater;
import org.codehaus.wadi.impl.CommonsHttpProxy;
import org.codehaus.wadi.impl.StandardHttpProxy;
import org.codehaus.wadi.impl.WebHybridRelocater;
import org.codehaus.wadi.test.MyContext;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Unit Tests requiring a pair of Jetty's. Each one is set up with a Filter and Servlet placeholder.
 * These are injected with actual Filter and Servlet instances before the running of each test. This
 * allows the tests to set up the innards of these components, make http requests to them and then inspect
 * their innards for the expected changes,
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1846 $
 */

public abstract class AbstractTestRelocation extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());
	
	protected final String _clusterName="WADI.TEST";
	protected Node _node0;
	protected Node _node1;
	protected MyFilter _filter0;
	protected MyFilter _filter1;
	protected MyServlet _servlet0;
	protected MyServlet _servlet1;
	protected Location _location0;
	protected Location _location1;
	protected Dispatcher _dispatcher0;
	protected Dispatcher _dispatcher1;
	protected SwitchableRelocater _relocater0;
	protected SwitchableRelocater _relocater1;
	
	class SwitchableRelocater extends AbstractRelocater {
		protected Relocater _delegate=new DummyRelocater();
		
		public void setRelocationStrategy(Relocater delegate){
			_delegate=delegate;
			_delegate.init(_config);
		}
		
		// Relocater
		public boolean relocate(Invocation invocation, String name, Immoter immoter, Sync motionLock) throws InvocationException {
			return _delegate.relocate(invocation, name, immoter, motionLock);
		}
		
	}
	
	class DummyRelocater extends AbstractRelocater {
		public boolean relocate(Invocation invocation, String name, Immoter immoter, Sync motionLock) throws InvocationException {
			return false;
		}
		protected Contextualiser _top;
		public void setTop(Contextualiser top) {_top=top;}
		public Contextualiser getTop(){return _top;}
	}
	
	protected abstract Dispatcher createDispatcher(String clusterName, String nodeName, long timeout) throws Exception;
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		//ConnectionFactory connectionFactory = Utils.getConnectionFactory();
		//ClusterFactory clusterFactory       = new CustomClusterFactory(connectionFactory);
		//String clusterName                  = "ORG.CODEHAUS.WADI.TEST.CLUSTER";
		
		InetSocketAddress httpAddress0=new InetSocketAddress("localhost", 8080);
		InvocationProxy httpProxy0=new StandardHttpProxy("jsessionid");
		_relocater0=new SwitchableRelocater();
		_servlet0=new MyServlet("0", _clusterName, new MyContextPool(), _relocater0, httpProxy0, httpAddress0, createDispatcher(_clusterName, "0", 5000L));
		_filter0=new MyFilter("0", _servlet0);
		// TODO - I'd like to use a TomcatNode - but using 5.0.18 it fails TestRelocation - investigate...
		(_node0=new JettyNode("0", "localhost", 8080, "/test", "/home/jules/workspace/wadi/webapps/test", _filter0, _servlet0)).start();
		
		InetSocketAddress httpAddress1=new InetSocketAddress("localhost", 8081);
		InvocationProxy httpProxy1=new CommonsHttpProxy("jsessionid");
		_relocater1=new SwitchableRelocater();
		_servlet1=new MyServlet("1", _clusterName, new MyContextPool(), _relocater1, httpProxy1, httpAddress1, createDispatcher(_clusterName, "1", 5000L));
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
	public AbstractTestRelocation(String name) {
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
		_relocater0.setRelocationStrategy(new WebHybridRelocater(2000, 3000, true));
		_relocater1.setRelocationStrategy(new WebHybridRelocater(2000, 3000, true));
		testInsecureRelocation(false);
	}
	
	public void testMigrateInsecureRelocation() throws Exception {
		//Collapser collapser=new HashingCollapser(10, 2000);
		_relocater0.setRelocationStrategy(new WebHybridRelocater(2000, 500, true));
		_relocater1.setRelocationStrategy(new WebHybridRelocater(2000, 500, true));
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
		_filter0.setExclusiveOnly(true);
		assertTrue(get(client, method0, "/test;jsessionid=foo")!=200);
		assertTrue(get(client, method0, "/test;jsessionid=bar")!=200);
		_filter1.setExclusiveOnly(true);
		assertTrue(get(client, method1, "/test;jsessionid=foo")!=200);
		assertTrue(get(client, method1, "/test;jsessionid=bar")!=200);
		
		assertTrue(m0.isEmpty());
		assertTrue(m1.isEmpty());
		assertTrue(c0.isEmpty());
		assertTrue(c1.isEmpty());
		
		m0.put("foo", new MyContext("foo", "1"));
		m1.put("bar", new MyContext("bar", "2"));
		
		assertTrue(m0.size()==1);
		assertTrue(m1.size()==1);
		assertTrue(c0.isEmpty());
		assertTrue(c1.isEmpty());
		
		// 2/4 sessions available locally
		_filter0.setExclusiveOnly(true);
		int status=get(client, method0, "/test;jsessionid=foo");
		if (_log.isInfoEnabled()) _log.info("STATUS="+status);
		assertTrue(status==200);
		assertTrue(get(client, method0, "/test;jsessionid=bar")!=200);
		_filter1.setExclusiveOnly(true);
		assertTrue(get(client, method1, "/test;jsessionid=foo")!=200);
		assertTrue(get(client, method1, "/test;jsessionid=bar")==200);
		
		assertTrue(m0.size()==1);
		assertTrue(m1.size()==1);
		assertTrue(c0.isEmpty());
		assertTrue(c1.isEmpty());
		
		// 4/4 sessions available locally|remotely
		_filter0.setExclusiveOnly(false);
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
		
		_filter1.setExclusiveOnly(false);
		assertTrue(get(client, method1, "/test;jsessionid=foo")==200);
		assertTrue(get(client, method1, "/test;jsessionid=bar")==200);
		
		if (migrating) {
			assertTrue(m1.size()==2);
			Thread.sleep(1000); // can take a while for ACK to be processed
			assertTrue(m0.size()==0);
			assertTrue(c0.size()==2); // n0 should now know that both sessions are on n1
			if (_log.isInfoEnabled()) {
				_log.info("M0="+m0);
				_log.info("M1="+m1);
				_log.info("C0="+c0);
				_log.info("C1="+c1);
			}
			assertTrue(c1.size()==0); // n1 has all the sessions and doesn't need to know anything...
		} else {
			assertTrue(m0.size()==1);
			assertTrue(m1.size()==1);
			assertTrue(c0.size()==1); // location from clusterwide query has been cached
			assertTrue(c1.size()==1); // location from clusterwide query has been cached
		}
		
		// ensure that cached locations work second time around...
		_filter0.setExclusiveOnly(false);
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
		
		_filter1.setExclusiveOnly(false);
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
		_relocater0.setRelocationStrategy(new WebHybridRelocater(2000, 3000, true));
		_relocater1.setRelocationStrategy(new WebHybridRelocater(2000, 3000, true));
		testSecureRelocation(false);
	}
	
	public void testMigrateSecureRelocation() throws Exception {
		//Collapser collapser=new HashingCollapser(10, 2000);
		_relocater0.setRelocationStrategy(new WebHybridRelocater(2000, 500, true));
		_relocater1.setRelocationStrategy(new WebHybridRelocater(2000, 500, true));
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
		
		m0.put("foo", new MyContext("foo", "1"));
		m1.put("bar", new MyContext("bar", "2"));
		
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
		_relocater0.setRelocationStrategy(new WebHybridRelocater(2000, 3000, true));
		_relocater1.setRelocationStrategy(new WebHybridRelocater(2000, 3000, true));
		testStatelessContextualiser(false);
	}
	
	public void testMigrateStatelessContextualiser() throws Exception {
		//Collapser collapser=new HashingCollapser(10, 2000);
		_relocater0.setRelocationStrategy(new WebHybridRelocater(2000, 500, true));
		_relocater1.setRelocationStrategy(new WebHybridRelocater(2000, 500, true));
		testStatelessContextualiser(true);
	}
	
	public void testStatelessContextualiser(boolean migrating) throws Exception {
		
		HttpClient client=new HttpClient();
		HttpMethod method0=new GetMethod("http://localhost:8080");
		HttpMethod method1=new GetMethod("http://localhost:8081");
		
		Map m0=_servlet0.getMemoryMap();
		Map m1=_servlet1.getMemoryMap();
		
		m0.put("foo", new MyContext("foo", "1"));
		m1.put("bar", new MyContext("bar", "2"));
		
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
