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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jms.JMSException;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.ContextPool;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Location;
import org.codehaus.wadi.sandbox.RelocationStrategy;
import org.codehaus.wadi.sandbox.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.impl.MemoryContextualiser;
import org.codehaus.wadi.sandbox.impl.MessageDispatcher;
import org.codehaus.wadi.sandbox.impl.NeverEvicter;
import org.codehaus.wadi.sandbox.impl.SerialContextualiser;
import org.codehaus.wadi.sandbox.impl.StatelessContextualiser;
import org.codehaus.wadi.sandbox.impl.SwitchableEvicter;

public class MyServlet implements Servlet {
	protected ServletConfig _config;
	protected final Log _log;
	protected final Cluster _cluster;
	protected final Collapser _collapser;
	protected final Map _clusterMap;
	protected final Map _memoryMap;
	protected final MessageDispatcher _dispatcher;
	protected final RelocationStrategy _relocater;
	protected final ClusterContextualiser _clusterContextualiser;
	protected final StatelessContextualiser _statelessContextualiser;
	protected final SerialContextualiser _serialContextualiser;
	protected final MemoryContextualiser _memoryContextualiser;
	protected final Location _location;

	public MyServlet(String name, Cluster cluster, ContextPool contextPool, MessageDispatcher dispatcher, RelocationStrategy relocater, Location location) throws Exception {
		_log=LogFactory.getLog(getClass().getName()+"#"+name);
		_cluster=cluster;
		_cluster.start();
		_collapser=new HashingCollapser(10, 2000);
		_clusterMap=new HashMap();
		_dispatcher=dispatcher;
		_relocater=relocater;
		_location=location;
		_clusterContextualiser=new ClusterContextualiser(new DummyContextualiser(), _collapser, _clusterMap, new SwitchableEvicter(), _dispatcher, _relocater, _location);
		//(Contextualiser next, Pattern methods, boolean methodFlag, Pattern uris, boolean uriFlag)
		Pattern methods=Pattern.compile("GET|POST", Pattern.CASE_INSENSITIVE);
		Pattern uris=Pattern.compile(".*\\.(JPG|JPEG|GIF|PNG|ICO|HTML|HTM)(|;jsessionid=.*)", Pattern.CASE_INSENSITIVE);
		_statelessContextualiser=new StatelessContextualiser(_clusterContextualiser, methods, true, uris, false);
		_serialContextualiser=new SerialContextualiser(_statelessContextualiser, _collapser);
		_memoryMap=new HashMap();
		_memoryContextualiser=new MemoryContextualiser(_serialContextualiser, _collapser, _memoryMap, new NeverEvicter(), new SimpleStreamingStrategy(), contextPool);
		relocater.setTop(_memoryContextualiser);
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

		if (_test!=null)
			_test.test(req, res);
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

	interface Test {
		void test(ServletRequest req, ServletResponse res);
	}

	protected Test _test;
	public void setTest(Test test){_test=test;}

	public Map getClusterMap(){return _clusterMap;}
	public Map getMemoryMap(){return _memoryMap;}
}
