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
import java.util.HashMap;
import java.util.Map;

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
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.ContextPool;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.HttpProxy;
import org.codehaus.wadi.sandbox.context.Motable;
import org.codehaus.wadi.sandbox.context.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.context.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.context.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.context.impl.HttpProxyLocation;
import org.codehaus.wadi.sandbox.context.impl.MemoryContextualiser;
import org.codehaus.wadi.sandbox.context.impl.NeverEvicter;

public class MyServlet implements Servlet {
	protected ServletConfig _config;
	protected final Log _log;
	protected final Cluster _cluster;
	protected final Collapser _collapser;
	protected final Map _clusterMap;
	protected final Map _memoryMap;
	protected final ClusterContextualiser _clusterContextualiser;
	protected final MemoryContextualiser _memoryContextualiser;
	
	public MyServlet(String name, Cluster cluster, InetSocketAddress location, ContextPool contextPool, HttpProxy proxy) throws Exception {
		_log=LogFactory.getLog(getClass().getName()+"#"+name);
		_cluster=cluster;
		_cluster.start();
		_collapser=new HashingCollapser(10, 2000);
		_clusterMap=new HashMap();
		_clusterContextualiser=new ClusterContextualiser(new DummyContextualiser(), _collapser, _clusterMap, new MyEvicter(0), _cluster, 2000, 3000, new HttpProxyLocation(location, proxy));
		_memoryMap=new HashMap();
		_memoryContextualiser=new MemoryContextualiser(_clusterContextualiser, _collapser, _memoryMap, new NeverEvicter(), contextPool);
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
}