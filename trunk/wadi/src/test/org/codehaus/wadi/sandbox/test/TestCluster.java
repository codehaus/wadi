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

import java.io.File;
import java.net.InetSocketAddress;
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
import org.codehaus.wadi.IdGenerator;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.impl.TomcatIdGenerator;
import org.codehaus.wadi.sandbox.AttributesPool;
import org.codehaus.wadi.sandbox.Cluster;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.ContextPool;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.HttpProxy;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Location;
import org.codehaus.wadi.sandbox.Motable;
import org.codehaus.wadi.sandbox.RelocationStrategy;
import org.codehaus.wadi.sandbox.Router;
import org.codehaus.wadi.sandbox.SessionPool;
import org.codehaus.wadi.sandbox.SessionWrapperFactory;
import org.codehaus.wadi.sandbox.ValuePool;
import org.codehaus.wadi.sandbox.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.impl.CustomClusterFactory;
import org.codehaus.wadi.sandbox.impl.DistributableAttributesFactory;
import org.codehaus.wadi.sandbox.impl.DistributableManager;
import org.codehaus.wadi.sandbox.impl.DistributableSessionFactory;
import org.codehaus.wadi.sandbox.impl.DistributableValueFactory;
import org.codehaus.wadi.sandbox.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.impl.DummyEvicter;
import org.codehaus.wadi.sandbox.impl.DummyRouter;
import org.codehaus.wadi.sandbox.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.sandbox.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.impl.HttpProxyLocation;
import org.codehaus.wadi.sandbox.impl.Manager;
import org.codehaus.wadi.sandbox.impl.MemoryContextualiser;
import org.codehaus.wadi.sandbox.impl.MessageDispatcher;
import org.codehaus.wadi.sandbox.impl.RestartableClusterFactory;
import org.codehaus.wadi.sandbox.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.sandbox.impl.SharedJDBCContextualiser;
import org.codehaus.wadi.sandbox.impl.SharedJDBCMotable;
import org.codehaus.wadi.sandbox.impl.SimpleAttributesPool;
import org.codehaus.wadi.sandbox.impl.SimpleSessionPool;
import org.codehaus.wadi.sandbox.impl.SimpleValuePool;
import org.codehaus.wadi.sandbox.impl.StandardHttpProxy;
import org.codehaus.wadi.sandbox.impl.Utils;
import org.codehaus.wadi.sandbox.impl.jetty.JettySessionWrapperFactory;

/**
 * Test the shutdown of a Contextualiser stack as live sessions are distributed to other nodes in the cluster
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TestCluster extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());

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

    class MyNode {
    
        protected final Cluster _cluster;
        protected final MessageDispatcher _dispatcher;
        protected final Location _location;
        protected final RelocationStrategy _relocater;
        protected final Map _cmap=new HashMap();
        protected final Map _mmap=new HashMap();
        protected final Evicter _evicter=new DummyEvicter();
        protected final MemoryContextualiser _top;
        protected final ClusterContextualiser _middle;
        protected final SharedJDBCContextualiser _bottom;
        protected final Manager _manager;
        
        public MyNode(ClusterFactory factory, String clusterName, DataSource ds, String table) throws JMSException, ClusterException {
            _bottom=new SharedJDBCContextualiser(_dummyContextualiser, _collapser, ds, table);
            _cluster=(Cluster)factory.createCluster(clusterName);
            _cluster.addClusterListener(new MyClusterListener());
            _dispatcher=new MessageDispatcher(_cluster);
            InetSocketAddress isa=new InetSocketAddress("localhost", 8080);
            HttpProxy proxy=new StandardHttpProxy("jsessionid");
            _location=new HttpProxyLocation(_cluster.getLocalNode().getDestination(), isa, proxy);
            //_relocater=new SwitchableRelocationStrategy();
            _relocater=null;
            _middle=new ClusterContextualiser(_bottom, _collapser, new DummyEvicter(), _cmap, _cluster, _dispatcher, _relocater, _location);
            _top=new MemoryContextualiser(_middle, _evicter, _mmap, _streamer, _distributableContextPool, new DummyStatefulHttpServletRequestWrapperPool());
            _middle.setTop(_top);
            _manager=new DistributableManager(_distributableSessionPool, _distributableAttributesPool, _distributableValuePool, _sessionWrapperFactory, _sessionIdFactory, _top, _mmap, _router, _streamer, _accessOnLoad);
        }
        
        protected boolean _running;
        
        public synchronized void start() throws Exception {
            if (!_running) {
                _cluster.start();
                _manager.start();
                _running=true;
            }
        }
        
        public synchronized void stop() throws Exception {
            if (_running) {
                _log.info("stopping contextualiser stack...");
                _top.stop();
                _log.info("contextualiser stack stopped");
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
            SharedJDBCMotable.init(_ds, _table);
            
		(_node0=new MyNode(_clusterFactory, _clusterName, _ds, _table)).start();
		(_node1=new MyNode(_clusterFactory, _clusterName, _ds, _table)).start();
		(_node2=new MyNode(_clusterFactory, _clusterName, _ds, _table)).start();

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
//		_node2.stop();
		Thread.sleep(6000);
		_node1.stop();
//		Thread.sleep(6000);
		_node0.stop();
//		Thread.sleep(10000);
        
        if (!_preserveDB)
            SharedJDBCMotable.destroy(_ds, _table);
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
			Motable emotable=_distributableSessionPool.take();
            emotable.setId(id);
			Immoter immoter=c0.getDemoter(id, emotable);
			Emoter emoter=new EtherEmoter();
			Utils.mote(emoter, immoter, emotable, id);
		}
		assertTrue(m0.size()==numContexts); // all sessions are on node0

        // shutdown node0
        // sessions should be evacuated to remaining two nodes...
        _log.info("NODES: "+_node0.getCluster().getNodes().size());
		_node0.stop();
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
        _log.info("NODES: should be 0");
		Thread.sleep(6000); // give everything some time to happen...
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
