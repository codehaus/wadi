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
package org.codehaus.wadi.test;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.ContextPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.HttpProxy;
import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.Relocater;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.gridstate.PartitionManager;
import org.codehaus.wadi.gridstate.activecluster.ActiveClusterDispatcher;
import org.codehaus.wadi.gridstate.impl.DummyPartitionManager;
import org.codehaus.wadi.impl.ClusterContextualiser;
import org.codehaus.wadi.impl.DistributableAttributesFactory;
import org.codehaus.wadi.impl.ClusteredManager;
import org.codehaus.wadi.impl.DistributableSessionFactory;
import org.codehaus.wadi.impl.DistributableValueFactory;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyReplicaterFactory;
import org.codehaus.wadi.impl.DummyRouter;
import org.codehaus.wadi.impl.HashingCollapser;
import org.codehaus.wadi.impl.StandardManager;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.NeverEvicter;
import org.codehaus.wadi.impl.SerialContextualiser;
import org.codehaus.wadi.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.StandardSessionWrapperFactory;
import org.codehaus.wadi.impl.StatelessContextualiser;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;
import org.codehaus.wadi.impl.Utils;

public class MyServlet implements Servlet {
	protected ServletConfig _config;
	protected final Log _log;
    protected final String _clusterUri=Utils.getClusterUri();
    protected final String _clusterName;
    protected final String _nodeName;
	protected final Map _clusterMap;
	protected final Map _memoryMap;
	protected final Relocater _relocater;
	protected final ClusterContextualiser _clusterContextualiser;
	protected final StatelessContextualiser _statelessContextualiser;
	protected final SerialContextualiser _serialContextualiser;
	protected final MemoryContextualiser _memoryContextualiser;
    protected final Streamer _streamer=new SimpleStreamer();
    protected final Contextualiser _dummyContextualiser=new DummyContextualiser();
    protected final Collapser _collapser=new HashingCollapser(10, 2000);
    protected final SessionWrapperFactory _sessionWrapperFactory=new StandardSessionWrapperFactory();
    protected final SessionIdFactory _sessionIdFactory=new TomcatSessionIdFactory();
    protected final boolean _accessOnLoad=true;
    protected final Router _router=new DummyRouter();
    protected final SessionPool _distributableSessionPool=new SimpleSessionPool(new DistributableSessionFactory());
    protected final ContextPool _distributableContextPool=new SessionToContextPoolAdapter(_distributableSessionPool);
    protected final AttributesFactory _distributableAttributesFactory=new DistributableAttributesFactory();
    protected final ValuePool _distributableValuePool=new SimpleValuePool(new DistributableValueFactory());
    protected final HttpProxy _httpProxy;
    protected final InetSocketAddress _httpAddress;
    protected final StandardManager _manager;


	public MyServlet(String nodeName, String clusterName, ContextPool contextPool, Relocater relocater, HttpProxy httpProxy, InetSocketAddress httpAddress) throws Exception {
		_log=LogFactory.getLog(getClass().getName()+"#"+nodeName);
        _clusterName=clusterName;
        _nodeName=nodeName;
		_clusterMap=new HashMap();
		_relocater=relocater;
		_clusterContextualiser=new ClusterContextualiser(new DummyContextualiser(), _collapser, _relocater);
		//(Contextualiser next, Pattern methods, boolean methodFlag, Pattern uris, boolean uriFlag)
		Pattern methods=Pattern.compile("GET|POST", Pattern.CASE_INSENSITIVE);
		Pattern uris=Pattern.compile(".*\\.(JPG|JPEG|GIF|PNG|ICO|HTML|HTM)(|;jsessionid=.*)", Pattern.CASE_INSENSITIVE);
		_statelessContextualiser=new StatelessContextualiser(_clusterContextualiser, methods, true, uris, false);
		_memoryMap=new HashMap();
        _serialContextualiser=new SerialContextualiser(_statelessContextualiser, _collapser, _memoryMap);
		_memoryContextualiser=new MemoryContextualiser(_serialContextualiser, new NeverEvicter(30000, true), _memoryMap, new SimpleStreamer(), contextPool, new MyDummyHttpServletRequestWrapperPool());
        _httpProxy=httpProxy;
        _httpAddress=httpAddress;
        PartitionManager partitionManager=new DummyPartitionManager(24);
        Dispatcher dispatcher=new ActiveClusterDispatcher(_nodeName, _clusterName, _clusterUri, 5000L);
        _manager=new ClusteredManager(_distributableSessionPool, _distributableAttributesFactory, _distributableValuePool, _sessionWrapperFactory, _sessionIdFactory, _memoryContextualiser, _memoryMap, _router, true, _streamer, _accessOnLoad, new DummyReplicaterFactory(), _httpAddress, _httpProxy, dispatcher, partitionManager);
    }

	public Contextualiser getContextualiser(){return _memoryContextualiser;}

	public void init(ServletConfig config) {
		_config = config;
		_log.info("Servlet.init()");
        try {
            _manager.init(new ManagerConfig() {

				public ServletContext getServletContext() {
					return null;
				}

				public void callback(StandardManager manager) {
					// do nothing - should install Listeners...
				}

            });
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
        if (_log.isInfoEnabled()) _log.info("Servlet.service("+((sessionId==null)?"":sessionId)+")");

        if (_test!=null)
			_test.test(req, res);
	}

	public String getServletInfo() {
		return "Test Servlet";
	}

	public void destroy() {
		try {
            _manager.stop();
            _manager.destroy();
		} catch (Exception e) {
		  _log.warn("unexpected problem", e);
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
