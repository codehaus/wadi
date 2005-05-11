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

import java.io.File;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.sql.DataSource;

import junit.framework.TestCase;

import org.activecluster.ClusterException;
import org.activecluster.ClusterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.axiondb.jdbc.AxionDataSource;
import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Cluster;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.ContextPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.HttpProxy;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Relocater;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.impl.ClusterContextualiser;
import org.codehaus.wadi.impl.CustomClusterFactory;
import org.codehaus.wadi.impl.DistributableAttributesFactory;
import org.codehaus.wadi.impl.DistributableManager;
import org.codehaus.wadi.impl.DistributableSessionFactory;
import org.codehaus.wadi.impl.DistributableValueFactory;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyEvicter;
import org.codehaus.wadi.impl.DummyRelocater;
import org.codehaus.wadi.impl.DummyRouter;
import org.codehaus.wadi.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.impl.HashingCollapser;
import org.codehaus.wadi.impl.HttpProxyLocation;
import org.codehaus.wadi.impl.Manager;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.MessageDispatcher;
import org.codehaus.wadi.impl.RestartableClusterFactory;
import org.codehaus.wadi.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.impl.SharedStoreContextualiser;
import org.codehaus.wadi.impl.DatabaseMotable;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.StandardHttpProxy;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.impl.jetty.JettySessionWrapperFactory;

/**
 * Test the shutdown of a Contextualiser stack as live sessions are distributed to other nodes in the cluster
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TestCluster extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());

    protected final Streamer _streamer=new SimpleStreamer();
    protected final Contextualiser _dummyContextualiser=new DummyContextualiser();
    protected final Collapser _collapser=new HashingCollapser(10, 2000);
    protected final SessionWrapperFactory _sessionWrapperFactory=new JettySessionWrapperFactory();
    protected final SessionIdFactory _sessionIdFactory=new TomcatSessionIdFactory();
    protected final boolean _accessOnLoad=true;
    protected final Router _router=new DummyRouter();


    class MyNode {
    
        protected final Cluster _cluster;
        protected final MessageDispatcher _dispatcher;
        protected final Location _location;
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
        protected final Manager _manager;
        
        public MyNode(String nodeId, ClusterFactory factory, String clusterName, DataSource ds, String table) throws JMSException, ClusterException {
            _bottom=new SharedStoreContextualiser(_dummyContextualiser, _collapser, false, ds, table);
            _cluster=(Cluster)factory.createCluster(clusterName);
            _cluster.addClusterListener(new MyClusterListener());
            _dispatcher=new MessageDispatcher(_cluster);
            InetSocketAddress isa=new InetSocketAddress("localhost", 8080);
            HttpProxy proxy=new StandardHttpProxy("jsessionid");
            _location=new HttpProxyLocation(_cluster.getLocalNode().getDestination(), isa, proxy);
            //_relocater=new SwitchableRelocationStrategy();
            _relocater=new DummyRelocater();
            _middle=new ClusterContextualiser(_bottom, _collapser, new DummyEvicter(), _cmap, _cluster, _dispatcher, _relocater, _location, nodeId);
            _top=new MemoryContextualiser(_middle, _evicter, _mmap, _streamer, _distributableContextPool, new DummyStatefulHttpServletRequestWrapperPool());
            _middle.setTop(_top);
            _manager=new DistributableManager(_distributableSessionPool, _distributableAttributesFactory, _distributableValuePool, _sessionWrapperFactory, _sessionIdFactory, _top, _mmap, _router, _streamer, _accessOnLoad);
        }
        
        protected boolean _running;
        
        public synchronized void start() throws Exception {
            if (!_running) {
                _log.info("starting cluster...");
                _cluster.start();
                _log.info("cluster started");
                _manager.init();
                _manager.start();
                _running=true;
            }
        }
        
        public synchronized void stop() throws Exception {
            if (_running) {
                _manager.stop();
                _manager.destroy();
                _log.info("stopping cluster...");
                _cluster.stop();
                Thread.sleep(6000);
                _log.info("cluster stopped");
                _running=false;
            }
        }
        
        public Map getClusterContextualiserMap() {return _cmap;}
        public Cluster getCluster(){return _cluster;}
        public ClusterContextualiser getClusterContextualiser() {return _middle;}
        
        public Map getMemoryContextualiserMap() {return _mmap;}
        public MemoryContextualiser getMemoryContextualiser(){return _top;}
    }

	protected final ConnectionFactory _connectionFactory=Utils.getConnectionFactory();
	protected final ClusterFactory _clusterFactory=new RestartableClusterFactory(new CustomClusterFactory(_connectionFactory));
	protected final String _clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
    protected final DataSource _ds=new AxionDataSource("jdbc:axiondb:testdb");
    protected final String _table="WADISESSIONS";
    protected boolean _preserveDB;

    protected MyNode _node0;
    protected MyNode _node1;
    protected MyNode _node2;
    
    /**
     * Constructor for TestCluster.
     * @param name
     */
    public TestCluster(String name) throws Exception {
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
            DatabaseMotable.init(_ds, _table);
            
		(_node0=new MyNode("node0", _clusterFactory, _clusterName, _ds, _table)).start();
		(_node1=new MyNode("node1", _clusterFactory, _clusterName, _ds, _table)).start();
		(_node2=new MyNode("node2", _clusterFactory, _clusterName, _ds, _table)).start();

		//_node0.getCluster().waitForClusterToComplete(3, 6000);
		//_node1.getCluster().waitForClusterToComplete(3, 6000);
		_node2.getCluster().waitForClusterToComplete(3, 6000);
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
            DatabaseMotable.destroy(_ds, _table);
	}

	public void testEvacuation() throws Exception {
		Contextualiser c0=_node0.getMemoryContextualiser();

		Map m0=_node0.getMemoryContextualiserMap();
		Map m1=_node1.getMemoryContextualiserMap();
		Map m2=_node2.getMemoryContextualiserMap();
        

        // populate node0
		int numContexts=3;
		for (int i=0; i<numContexts; i++) {
			String id="session-"+i;
			Motable emotable=_node0._distributableSessionPool.take();
            long time=System.currentTimeMillis();
            emotable.init(time, time, 30*60, id);
			Immoter immoter=c0.getDemoter(id, emotable);
			Emoter emoter=new EtherEmoter();
			Utils.mote(emoter, immoter, emotable, id);
		}
		assertTrue(m0.size()==numContexts); // all sessions are on node0

        // shutdown node0
        // sessions should be evacuated to remaining two nodes...
        _log.info("NODES: "+_node0.getCluster().getNodes().size());
		_node0.stop();
        Thread.sleep(6000); // time for other nodes to notice...
        _log.info("NODES: "+_node1.getCluster().getNodes().size());

		// where are all the sessions now ?
		// the sum of nodes 1 and 2 should total n Contexts
		{
		    int s0=m0.size();
		    int s1=m1.size();
		    int s2=m2.size();
		    _log.info("dispersal - n0:"+s0+", n1:"+s1+", n2:"+s2);
		    assertTrue(s0==0);
		    assertTrue(s1+s2==numContexts);
            // TODO - hmmmm...
		    assertTrue(_node0.getClusterContextualiserMap().size()==numContexts); // node0 remembers where everything was sent...
		}

        // shutdown node1
        // sessions should all be evacuated to node2
        _log.info("NODES: "+_node1.getCluster().getNodes().size());
		_node1.stop();
        Thread.sleep(6000); // time for other nodes to notice...
        _log.info("NODES: "+_node2.getCluster().getNodes().size());
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
        _log.info("NODES: "+_node2.getCluster().getNodes().size());
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
