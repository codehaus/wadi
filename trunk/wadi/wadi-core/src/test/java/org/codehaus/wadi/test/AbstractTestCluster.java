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

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.axiondb.jdbc.AxionDataSource;
import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.ContextPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.ProxiedLocation;
import org.codehaus.wadi.Relocater;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.ClusterContextualiser;
import org.codehaus.wadi.impl.ClusteredManager;
import org.codehaus.wadi.impl.DatabaseStore;
import org.codehaus.wadi.impl.DistributableAttributesFactory;
import org.codehaus.wadi.impl.DistributableSessionFactory;
import org.codehaus.wadi.impl.DistributableValueFactory;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyEvicter;
import org.codehaus.wadi.impl.DummyRelocater;
import org.codehaus.wadi.impl.DummyReplicaterFactory;
import org.codehaus.wadi.impl.DummyRouter;
import org.codehaus.wadi.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.impl.HashingCollapser;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.impl.SharedStoreContextualiser;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.StandardHttpProxy;
import org.codehaus.wadi.impl.StandardSessionWrapperFactory;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.web.WebProxiedLocation;

/**
 * Test the shutdown of a Contextualiser stack as live sessions are distributed to other nodes in the cluster
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class AbstractTestCluster extends TestCase {
	
	protected Log _log = LogFactory.getLog(getClass());

	protected final Streamer _streamer=new SimpleStreamer();
	protected final Contextualiser _dummyContextualiser=new DummyContextualiser();
	protected final Collapser _collapser=new HashingCollapser(10, 2000);
	protected final SessionWrapperFactory _sessionWrapperFactory=new StandardSessionWrapperFactory();
	protected final SessionIdFactory _sessionIdFactory=new TomcatSessionIdFactory();
	protected final boolean _accessOnLoad=true;
	protected final Router _router=new DummyRouter();

	protected abstract Dispatcher createDispatcher(String clusterName, String nodeName, long timeout) throws Exception;
	
    class MyNode {

		protected final String _clusterName;
		protected final String _nodeName;
		protected final Dispatcher _dispatcher;
		protected final Relocater _relocater;
		protected final Map _cmap=new HashMap();
		protected final Map _mmap=new HashMap();
		protected final Evicter _evicter=new DummyEvicter();
		protected final MemoryContextualiser _top;
		protected final ClusterContextualiser _middle;
		protected final SharedStoreContextualiser _bottom;
		protected final SessionPool _distributableSessionPool=new SimpleSessionPool(new DistributableSessionFactory());
		protected final ContextPool _distributableContextPool=new SessionToContextPoolAdapter(_distributableSessionPool);
		protected final AttributesFactory _distributableAttributesFactory=new DistributableAttributesFactory();
		protected final ValuePool _distributableValuePool=new SimpleValuePool(new DistributableValueFactory());
		protected final ClusteredManager _manager;

		public MyNode(String nodeName, String clusterName, DatabaseStore store) throws Exception {
			_bottom=new SharedStoreContextualiser(_dummyContextualiser, _collapser, false, store);
			_clusterName=clusterName;
			_nodeName=nodeName;
			_dispatcher=createDispatcher(_clusterName, _nodeName, 5000L);
			ProxiedLocation proxiedLocation= new WebProxiedLocation(new InetSocketAddress("localhost", 8080));
			InvocationProxy proxy=new StandardHttpProxy("jsessionid");
			//_relocater=new SwitchableRelocationStrategy();
			_relocater=new DummyRelocater();
			_middle=new ClusterContextualiser(_bottom, _collapser, _relocater);
			_top=new MemoryContextualiser(_middle, _evicter, _mmap, _streamer, _distributableContextPool, new DummyStatefulHttpServletRequestWrapperPool());
			_manager=new ClusteredManager(_distributableSessionPool, _distributableAttributesFactory, _distributableValuePool, _sessionWrapperFactory, _sessionIdFactory, _top, _mmap, _router, true, _streamer, _accessOnLoad, new DummyReplicaterFactory(), proxiedLocation, proxy, _dispatcher, 24, _collapser);
		}

		protected boolean _running;

		public synchronized void start() throws Exception {
			if (!_running) {
				_manager.init(new ManagerConfig() {

					public ServletContext getServletContext() {
						return null;
					}

					public void callback(Manager manager) {
						// do nothing - should install Listeners
					}

				});
				_manager.start();
				_running=true;
			}
		}

		public synchronized void stop() throws Exception {
			if (_running) {
				_manager.stop();
				_manager.destroy();
				Thread.sleep(6000);
				_running=false;
			}
		}

		public Map getClusterContextualiserMap() {return _cmap;}
		public Cluster getCluster(){return _dispatcher.getCluster();}
		public ClusterContextualiser getClusterContextualiser() {return _middle;}

		public Map getMemoryContextualiserMap() {return _mmap;}
		public MemoryContextualiser getMemoryContextualiser(){return _top;}
	}

	protected final String _clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
	protected final DataSource _ds=new AxionDataSource("jdbc:axiondb:testdb");
	protected final String _table="WADISESSIONS";
	protected final DatabaseStore _store=new DatabaseStore("test", _ds, _table, false, false, false);

	protected boolean _preserveDB;

	protected MyNode _node0;
	protected MyNode _node1;
	protected MyNode _node2;

	/**
	 * Constructor for TestCluster.
	 * @param name
	 */
	public AbstractTestCluster(String name) throws Exception {
		super(name);

		String preserveDB=System.getProperty("preserve.db");
		if (preserveDB!=null && preserveDB.equals("true"))
			_preserveDB=true;

		_log.info("TEST CTOR!");
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();

		if (!_preserveDB)
			_store.init();

		(_node0=new MyNode("node0", _clusterName, _store)).start();
		(_node1=new MyNode("node1", _clusterName, _store)).start();
		(_node2=new MyNode("node2", _clusterName, _store)).start();

		//_node0.getCluster().waitForClusterToComplete(3, 6000);
		//_node1.getCluster().waitForClusterToComplete(3, 6000);
		_node2.getCluster().waitOnMembershipCount(4, 6000);
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		Thread.sleep(1000);

		super.tearDown();
		_node2.stop();
		Thread.sleep(6000);
		_node1.stop();
//		Thread.sleep(6000);
		_node0.stop();
//		Thread.sleep(10000);

		if (!_preserveDB)
			_store.destroy();
	}

	public void testEvacuation() throws Exception {
		Map m0=_node0.getMemoryContextualiserMap();
		Map m1=_node1.getMemoryContextualiserMap();
		Map m2=_node2.getMemoryContextualiserMap();

		// populate node0
		Contextualiser c0=_node0.getMemoryContextualiser();

		int numContexts=3;
		for (int i=0; i<numContexts; i++) {
			String name="session-"+i;
			Motable emotable=_node0._distributableSessionPool.take();
			long time=System.currentTimeMillis();
			emotable.init(time, time, 30*60, name);
			Immoter immoter=c0.getDemoter(name, emotable);
			Emoter emoter=new EtherEmoter();
			Utils.mote(emoter, immoter, emotable, name);
		}
		assertTrue(m0.size()==numContexts); // all sessions are on node0

		// shutdown node0
		// sessions should be evacuated to remaining two nodes...
		if (_log.isInfoEnabled()) _log.info("NODES: " + _node0.getCluster().getRemotePeers().size());
		_node0.stop();
		Thread.sleep(6000); // time for other nodes to notice...
		if (_log.isInfoEnabled()) _log.info("NODES: " + _node1.getCluster().getRemotePeers().size());

		// where are all the sessions now ?
		// the sum of nodes 1 and 2 should total n Contexts
		{
			int s0=m0.size();
			int s1=m1.size();
			int s2=m2.size();
			if (_log.isInfoEnabled()) _log.info("dispersal - n0:" + s0 + ", n1:" + s1 + ", n2:" + s2);
			assertTrue(s0==0);
			assertTrue(s1+s2==numContexts);
			// TODO - hmmmm...
			assertTrue(_node0.getClusterContextualiserMap().size()==numContexts); // node0 remembers where everything was sent...
		}

		// shutdown node1
		// sessions should all be evacuated to node2
		if (_log.isInfoEnabled()) _log.info("NODES: " + _node1.getCluster().getRemotePeers().size());
		_node1.stop();
		Thread.sleep(6000); // time for other nodes to notice...
		if (_log.isInfoEnabled()) _log.info("NODES: " + _node2.getCluster().getRemotePeers().size());
		{
			int s0=m0.size();
			int s1=m1.size();
			int s2=m2.size();
			_log.info("dispersal - n0:"+s0+", n1:"+s1+", n2:"+s2);
			assertTrue(s0==0);
			assertTrue(s1==0);
			assertTrue(s2==numContexts);
			// TODO - hmmmm...
			//assertTrue(_node0.getClusterContextualiserMap().size()==numContexts); // node0 remembers where everything was sent...
		}

		// shutdown node2
		if (_log.isInfoEnabled()) _log.info("NODES: " + _node2.getCluster().getRemotePeers().size());
		_node2.stop();
		Thread.sleep(6000); // time for other nodes to notice...
		_log.info("NODES: should be 0");
		{
			int s0=m0.size();
			int s1=m1.size();
			int s2=m2.size();
			_log.info("dispersal - n0:"+s0+", n1:"+s1+", n2:"+s2);
			assertTrue(s0==0);
			assertTrue(s1==0);
			assertTrue(s2==0);
			//assertTrue(_node0.getClusterContextualiserMap().size()==numContexts); // node0 remembers where everything was sent...
		}

		// TODO - figure out what should happen to location caches, implement and test it.

		// restart nodes - first one up should reload saved contexts...

		assertTrue(m0.size()==0);
		_node0.start();
		assertTrue(m0.size()==numContexts);

		assertTrue(m1.size()==0);
		_node1.start();
		assertTrue(m1.size()==0);

		assertTrue(m2.size()==0);
		_node2.start();
		assertTrue(m2.size()==0);

	}
}
