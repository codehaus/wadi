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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.IdGenerator;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.impl.TomcatIdGenerator;
import org.codehaus.wadi.sandbox.AttributesPool;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.ContextPool;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Location;
import org.codehaus.wadi.sandbox.RelocationStrategy;
import org.codehaus.wadi.sandbox.Router;
import org.codehaus.wadi.sandbox.SessionPool;
import org.codehaus.wadi.sandbox.SessionWrapperFactory;
import org.codehaus.wadi.sandbox.ValuePool;
import org.codehaus.wadi.sandbox.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.impl.CustomCluster;
import org.codehaus.wadi.sandbox.impl.DistributableAttributesFactory;
import org.codehaus.wadi.sandbox.impl.DistributableManager;
import org.codehaus.wadi.sandbox.impl.DistributableSessionFactory;
import org.codehaus.wadi.sandbox.impl.DistributableValueFactory;
import org.codehaus.wadi.sandbox.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.impl.DummyRouter;
import org.codehaus.wadi.sandbox.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.impl.Manager;
import org.codehaus.wadi.sandbox.impl.MemoryContextualiser;
import org.codehaus.wadi.sandbox.impl.MessageDispatcher;
import org.codehaus.wadi.sandbox.impl.NeverEvicter;
import org.codehaus.wadi.sandbox.impl.SerialContextualiser;
import org.codehaus.wadi.sandbox.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.sandbox.impl.SimpleAttributesPool;
import org.codehaus.wadi.sandbox.impl.SimpleSessionPool;
import org.codehaus.wadi.sandbox.impl.SimpleValuePool;
import org.codehaus.wadi.sandbox.impl.StatelessContextualiser;
import org.codehaus.wadi.sandbox.impl.jetty.JettySessionWrapperFactory;

public class MyServlet implements Servlet {
	protected ServletConfig _config;
	protected final Log _log;
	protected final CustomCluster _cluster;
	protected final Map _clusterMap;
	protected final Map _memoryMap;
	protected final MessageDispatcher _dispatcher;
	protected final RelocationStrategy _relocater;
	protected final ClusterContextualiser _clusterContextualiser;
	protected final StatelessContextualiser _statelessContextualiser;
	protected final SerialContextualiser _serialContextualiser;
	protected final MemoryContextualiser _memoryContextualiser;
	protected final Location _location;
    protected final StreamingStrategy _streamer=new SimpleStreamingStrategy();
    protected final Contextualiser _dummyContextualiser=new DummyContextualiser();
    protected final Collapser _collapser=new HashingCollapser(10, 2000);
    protected final SessionWrapperFactory _sessionWrapperFactory=new JettySessionWrapperFactory();
    protected final IdGenerator _sessionIdFactory=new TomcatIdGenerator();
    protected final boolean _accessOnLoad=true;
    protected final Router _router=new DummyRouter();
    protected final SessionPool _distributableSessionPool=new SimpleSessionPool(new DistributableSessionFactory()); 
    protected final ContextPool _distributableContextPool=new SessionToContextPoolAdapter(_distributableSessionPool); 
    protected final AttributesPool _distributableAttributesPool=new SimpleAttributesPool(new DistributableAttributesFactory());
    protected final ValuePool _distributableValuePool=new SimpleValuePool(new DistributableValueFactory());
    protected final Manager _manager;


	public MyServlet(String name, CustomCluster cluster, ContextPool contextPool, MessageDispatcher dispatcher, RelocationStrategy relocater, Location location) throws Exception {
		_log=LogFactory.getLog(getClass().getName()+"#"+name);
		_cluster=cluster;
		_cluster.start();
		_clusterMap=new HashMap();
		_dispatcher=dispatcher;
		_relocater=relocater;
		_location=location;
		_clusterContextualiser=new ClusterContextualiser(new DummyContextualiser(), _collapser, new SwitchableEvicter(30000, true), _clusterMap, _cluster, _dispatcher, _relocater, _location);
		//(Contextualiser next, Pattern methods, boolean methodFlag, Pattern uris, boolean uriFlag)
		Pattern methods=Pattern.compile("GET|POST", Pattern.CASE_INSENSITIVE);
		Pattern uris=Pattern.compile(".*\\.(JPG|JPEG|GIF|PNG|ICO|HTML|HTM)(|;jsessionid=.*)", Pattern.CASE_INSENSITIVE);
		_statelessContextualiser=new StatelessContextualiser(_clusterContextualiser, methods, true, uris, false);
		_memoryMap=new HashMap();
        _serialContextualiser=new SerialContextualiser(_statelessContextualiser, _collapser, _memoryMap);
		_memoryContextualiser=new MemoryContextualiser(_serialContextualiser, new NeverEvicter(30000, true), _memoryMap, new SimpleStreamingStrategy(), contextPool, new MyDummyHttpServletRequestWrapperPool());
        _clusterContextualiser.setTop(_memoryContextualiser);
		relocater.setTop(_memoryContextualiser);
        _manager=new DistributableManager(_distributableSessionPool, _distributableAttributesPool, _distributableValuePool, _sessionWrapperFactory, _sessionIdFactory, _memoryContextualiser, _memoryMap, _router, _streamer, _accessOnLoad);
	}

	public Contextualiser getContextualiser(){return _memoryContextualiser;}

	public void init(ServletConfig config) {
		_config = config;
		_log.info("Servlet.init()");
        try {
            _manager.start();
        } catch (Exception e) {
            _log.warn(e);
        }
	}

	public ServletConfig getServletConfig() {
		return _config;
	}

	public void service(ServletRequest req, ServletResponse res) {
		String sessionId=((HttpServletRequest)req).getRequestedSessionId();
		_log.info("Servlet.service("+((sessionId==null)?"":sessionId)+")");

		if (_test!=null)
			_test.test(req, res);
	}

	public String getServletInfo() {
		return "Test Servlet";
	}

	public void destroy() {
		try {
            _memoryContextualiser.stop();
            _cluster.stop();
		} catch (Exception e) {
			_log.warn(e);
		}
	}

	interface Test {
		void test(ServletRequest req, ServletResponse res);
	}

	protected Test _test;
	public void setTest(Test test){_test=test;}

	public Map getClusterMap(){return _clusterMap;}
	public Map getMemoryMap(){return _memoryMap;}
}
