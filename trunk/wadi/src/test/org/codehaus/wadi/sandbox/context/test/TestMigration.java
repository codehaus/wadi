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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;
import org.codehaus.activecluster.ClusterFactory;
import org.codehaus.activecluster.impl.DefaultClusterFactory;
import org.codehaus.activemq.ActiveMQConnectionFactory;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.ContextPool;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Motable;
import org.codehaus.wadi.sandbox.context.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.context.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.context.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.context.impl.HttpProxyLocation;
import org.codehaus.wadi.sandbox.context.impl.MemoryContextualiser;
import org.codehaus.wadi.sandbox.context.impl.NeverEvicter;
import org.codehaus.wadi.sandbox.context.impl.StandardHttpProxy;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.jetty.servlet.WebApplicationHandler;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

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
	
	class Node {
		protected final Log _log;
		protected final Server _server=new Server();
		protected final SocketListener _listener=new SocketListener();
		protected final WebApplicationContext _context=new WebApplicationContext();
		protected final WebApplicationHandler _handler=new WebApplicationHandler();
		protected final FilterHolder _filterHolder;
		protected final ServletHolder _servletHolder;
		protected final Filter _filter;
		protected final Servlet _servlet;
		
		public Node(String name, String host, int port, String context, String pathSpec, Filter filter, Servlet servlet) throws UnknownHostException {
			_log=LogFactory.getLog(getClass().getName()+"#"+name);
			// filter
			String filterName="Filter";
			_filterHolder=new FilterHolder(_handler, filterName, FilterInstance.class.getName());
			_handler.addFilterHolder(_filterHolder);
			_handler.addFilterPathMapping(pathSpec, filterName, FilterHolder.__REQUEST);
			//servlet
			String servletName="Servlet";
			_servletHolder=new ServletHolder(_handler, servletName, ServletInstance.class.getName());
			_handler.addServletHolder(_servletHolder);
			_handler.mapPathToServlet(pathSpec, servletName);
			// handler
			_context.addHandler(_handler);
			// context
			_context.setContextPath(context);
			_server.addContext(_context);
			// listener
			_listener.setHost(host);
			_listener.setPort(port);
			_server.addListener(_listener);
			
			_filter=filter;
			_servlet=servlet;
		}

		public Filter getFilter(){return _filter;}
		public Servlet getServlet(){return _servlet;}
		
		public void start() throws Exception {
			_server.start();
			((FilterInstance)_filterHolder.getFilter()).setInstance(_filter);
			((ServletInstance)_servletHolder.getServlet()).setInstance(_servlet);
		}
		
		public void stop() throws Exception {
			_server.stop();
		}
	}
	
	public class TestFilter implements Filter {
		protected final Log _log;
		protected final TestServlet _servlet;
		
		public TestFilter(String name, TestServlet servlet) {
			_log=LogFactory.getLog(getClass().getName()+"#"+name);
			_servlet=servlet;
		}
		
		public void init(FilterConfig config) throws ServletException {
			_log.info("Filter.init()");
		}
		
		public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
		throws ServletException, IOException {
			String sessionId=((HttpServletRequest)req).getRequestedSessionId();
			_log.info("Filter.doFilter("+((sessionId==null)?"":sessionId)+")");
			boolean found=_servlet.getContextualiser().contextualise(req, res, chain, sessionId, null, null, _localOnly);
			
			if (!found)
				((HttpServletResponse)res).sendError(410, "could not locate session: "+sessionId);
		}
		
		public void destroy() {}
		
		protected boolean _localOnly=false;
		public void setLocalOnly(boolean localOnly){_localOnly=localOnly;}
	}
	
	class MyEvicter implements Evicter{
		long _remaining;

		MyEvicter(long remaining) {
			_remaining=remaining;
		}

		public boolean evict(String id, Motable m) {
			long expiry=m.getExpiryTime();
			long current=System.currentTimeMillis();
			long left=expiry-current;
			boolean evict=(left<=_remaining);

			//_log.info((!evict?"not ":"")+"evicting: "+id);

			return evict;
		}
	}

	class MyContext implements Context {
		String _val;
		ReadWriteLock _lock=new ReaderPreferenceReadWriteLock();
		long _expiryTime;

		MyContext(String val) {
			this(val, System.currentTimeMillis()+(30*1000));
		}

		MyContext(String val, long expiryTime) {
			_val=val;
			_expiryTime=expiryTime;
		}

		MyContext() {}

		public Sync getSharedLock(){return _lock.readLock();}
		public Sync getExclusiveLock(){return _lock.writeLock();}

		// Motable...

		public long getExpiryTime(){return _expiryTime;}

		// SerializableContext...

		public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
			_val=(String)oi.readObject();
		}

		public void writeContent(ObjectOutput oo) throws IOException, ClassNotFoundException {
			oo.writeObject(_val);
		}
	}

	class MyContextPool implements ContextPool {
		public void put(Context context){}
		public Context take(){return new MyContext();}
	}

	public class TestServlet implements Servlet {
		protected ServletConfig _config;
		protected final Log _log;
		protected final Cluster _cluster;
		protected final Collapser _collapser;
		protected final Map _clusterMap;
		protected final Map _memoryMap;
		protected final ClusterContextualiser _clusterContextualiser;
		protected final MemoryContextualiser _memoryContextualiser;
		
		public TestServlet(String name, Cluster cluster, InetSocketAddress location) throws Exception {
			_log=LogFactory.getLog(getClass().getName()+"#"+name);
			_cluster=cluster;
			_cluster.start();
			_collapser=new HashingCollapser(10, 2000);
			_clusterMap=new HashMap();
			_clusterContextualiser=new ClusterContextualiser(new DummyContextualiser(), _collapser, _clusterMap, new MyEvicter(0), _cluster, 2000, 3000, new HttpProxyLocation(location, new StandardHttpProxy()));
			_memoryMap=new HashMap();
			_memoryContextualiser=new MemoryContextualiser(_clusterContextualiser, _collapser, _memoryMap, new NeverEvicter(), new MyContextPool());
			_clusterContextualiser.setContextualiser(_memoryContextualiser);
		}
		
		public Contextualiser getContextualiser(){return _memoryContextualiser;}
		
		public void init(ServletConfig config) throws ServletException {
			_config = config;
			_log.info("Servlet.init()");
		}

		public ServletConfig getServletConfig() {
			return _config;
		}

		public void service(ServletRequest req, ServletResponse res)
				throws ServletException, IOException {
			String sessionId=((HttpServletRequest)req).getRequestedSessionId();
			_log.info("Servlet.service("+((sessionId==null)?"":sessionId)+")");
		}

		public String getServletInfo() {
			return "Test Servlet";
		}

		public void destroy() {
			try {
			_cluster.stop();
			} catch (JMSException e) {
				_log.warn(e);
			}
		}
		
		public Map getClusterMap(){return _clusterMap;}
		public Map getMemoryMap(){return _memoryMap;}
	}
	
	protected Node _node0;
	protected Node _node1;
	protected TestFilter _filter0;
	protected TestFilter _filter1;
	protected TestServlet _servlet0;
	protected TestServlet _servlet1;
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		System.setProperty("org.mortbay.xml.XmlParser.NotValidating", "true");
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://WADI-TEST");
		ClusterFactory clusterFactory       = new DefaultClusterFactory(connectionFactory);
		String clusterName                  = "ORG.CODEHAUS.WADI.TEST.CLUSTER";
		_servlet0=new TestServlet("0", clusterFactory.createCluster(clusterName), new InetSocketAddress("localhost", 8080));
		_filter0=new TestFilter("0", _servlet0);
		(_node0=new Node("0", "localhost", 8080, "/test", "/*", _filter0, _servlet0)).start();
		_servlet1=new TestServlet("1", clusterFactory.createCluster(clusterName), new InetSocketAddress("localhost", 8081));
		_filter1=new TestFilter("1", _servlet1);
		(_node1=new Node("1", "localhost", 8081, "/test", "/*", _filter1, _servlet1)).start();
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
		
		// next test should be that we can somehow migrate sessions across, in place of proxying...
		
		// TODO:
		// streamline API between HttpProxyLocation and HttpProxy
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
