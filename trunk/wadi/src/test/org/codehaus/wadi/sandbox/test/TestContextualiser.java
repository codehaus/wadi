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
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import junit.framework.TestCase;

import org.activecluster.ClusterFactory;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.impl.BrokerContainerFactoryImpl;
import org.activemq.store.vm.VMPersistenceAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.axiondb.jdbc.AxionDataSource;
import org.codehaus.wadi.sandbox.AttributesPool;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.Context;
import org.codehaus.wadi.sandbox.ContextPool;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Evictable;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.HttpServletRequestWrapperPool;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Location;
import org.codehaus.wadi.sandbox.Motable;
import org.codehaus.wadi.sandbox.RelocationStrategy;
import org.codehaus.wadi.sandbox.Router;
import org.codehaus.wadi.sandbox.Session;
import org.codehaus.wadi.sandbox.SessionIdFactory;
import org.codehaus.wadi.sandbox.SessionPool;
import org.codehaus.wadi.sandbox.SessionWrapperFactory;
import org.codehaus.wadi.sandbox.Streamer;
import org.codehaus.wadi.sandbox.ValuePool;
import org.codehaus.wadi.sandbox.impl.AbsoluteEvicter;
import org.codehaus.wadi.sandbox.impl.AbstractContextualiser;
import org.codehaus.wadi.sandbox.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.impl.CustomCluster;
import org.codehaus.wadi.sandbox.impl.CustomClusterFactory;
import org.codehaus.wadi.sandbox.impl.DistributableAttributesFactory;
import org.codehaus.wadi.sandbox.impl.DistributableManager;
import org.codehaus.wadi.sandbox.impl.DistributableSessionFactory;
import org.codehaus.wadi.sandbox.impl.DistributableValueFactory;
import org.codehaus.wadi.sandbox.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.impl.DummyEvicter;
import org.codehaus.wadi.sandbox.impl.DummyHttpServletRequest;
import org.codehaus.wadi.sandbox.impl.DummyRouter;
import org.codehaus.wadi.sandbox.impl.ExclusiveDiscContextualiser;
import org.codehaus.wadi.sandbox.impl.GZIPStreamer;
import org.codehaus.wadi.sandbox.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.impl.Manager;
import org.codehaus.wadi.sandbox.impl.MemoryContextualiser;
import org.codehaus.wadi.sandbox.impl.MessageDispatcher;
import org.codehaus.wadi.sandbox.impl.NeverEvicter;
import org.codehaus.wadi.sandbox.impl.ProxyRelocationStrategy;
import org.codehaus.wadi.sandbox.impl.SerialContextualiser;
import org.codehaus.wadi.sandbox.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.sandbox.impl.SharedJDBCContextualiser;
import org.codehaus.wadi.sandbox.impl.SharedJDBCMotable;
import org.codehaus.wadi.sandbox.impl.SimpleAttributesPool;
import org.codehaus.wadi.sandbox.impl.SimpleContextualiserStack;
import org.codehaus.wadi.sandbox.impl.SimpleEvictable;
import org.codehaus.wadi.sandbox.impl.SimpleSessionPool;
import org.codehaus.wadi.sandbox.impl.SimpleStreamer;
import org.codehaus.wadi.sandbox.impl.SimpleValuePool;
import org.codehaus.wadi.sandbox.impl.StandardAttributesFactory;
import org.codehaus.wadi.sandbox.impl.StandardSessionFactory;
import org.codehaus.wadi.sandbox.impl.StandardValueFactory;
import org.codehaus.wadi.sandbox.impl.TomcatSessionIdFactory;
import org.codehaus.wadi.sandbox.impl.Utils;
import org.codehaus.wadi.sandbox.impl.jetty.JettySessionWrapperFactory;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Test various Contualisers, evicters etc...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TestContextualiser extends TestCase {
    protected Log _log = LogFactory.getLog(getClass());
    protected DataSource _ds=new AxionDataSource("jdbc:axiondb:testdb");	// db springs into existance in-vm beneath us
    protected String _table="MyTable";
    
    protected final HttpServletRequest _request=new DummyHttpServletRequest();
    protected final HttpServletRequestWrapperPool _requestPool=new MyDummyHttpServletRequestWrapperPool();
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        _log.info("starting ...");
        SharedJDBCMotable.init(_ds, _table);
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        SharedJDBCMotable.destroy(_ds, _table);
        _log.info("...stopped");
    }
    
    /**
     * Constructor for TestContextualiser.
     * @param arg0
     */
    public TestContextualiser(String arg0) {
        super(arg0);
    }
    
    // test how interning works - we will use locking of keys to ensure that concurrent loading of same session does not occur...
    
    public void testIntern() {
        String s1="foo"; // automatically interned
        
        String s2=s1.intern();
        assertTrue(s2==s1);
        
        String s3=s1.intern();
        assertTrue(s3==s2);
        
        String s4="fo"+"o"; // automatically interned
        assertTrue(s4==s3);
        
        String s5=new StringBuffer("fo").append("o").toString(); // NOT interned
        assertTrue(s5!=s4);
        s5=s5.intern();
        assertTrue(s5==s4); // now it is
        
        String s6="-foo-".substring(1, 4); // NOT interned
        assertTrue(s6!=s5);
        
        // etc...
    }
    
    class MyFilterChain
    implements FilterChain
    {
        public void
        doFilter(ServletRequest request, ServletResponse response) {
            _log.info("invoking FilterChain...");
        }
    }
    
    protected final Evicter _dummyEvicter=new DummyEvicter();
    
    public void testExclusivePromotion() throws Exception {
        Map d2=new HashMap();
        ExclusiveDiscContextualiser disc2=new ExclusiveDiscContextualiser(_dummyContextualiser, _collapser, _dummyEvicter, d2, _streamer, _dir);
        Map d1=new HashMap();
        ExclusiveDiscContextualiser disc1=new ExclusiveDiscContextualiser(disc2, _collapser, _dummyEvicter, d1, _streamer, _dir);
        Map m=new HashMap();
        Contextualiser serial=new SerialContextualiser(disc1, _collapser, m);
        Contextualiser memory=new MemoryContextualiser(serial, _dummyEvicter, m, _streamer, _distributableContextPool, _requestPool);
        new DistributableManager(_distributableSessionPool, _distributableAttributesPool, _distributableValuePool, _sessionWrapperFactory, _sessionIdFactory, memory, m, _router, _streamer, _accessOnLoad);
        
        {
            // place a "baz" item onto second local disc
            String id="baz";
            Motable emotable=_distributableSessionPool.take();
            emotable.setId(id);
            emotable.setCreationTime(3);
            emotable.setMaxInactiveInterval(6);
            Immoter immoter=disc2.getDemoter(id, emotable);
            Emoter emoter=new EtherEmoter();
            Utils.mote(emoter, immoter, emotable, id);
            assertTrue(d2.containsKey(id));
            assertTrue(d2.size()==1);
        }
        {
            // place a "bar" item onto first local disc
            String id="bar";
            Motable emotable=_distributableSessionPool.take();
            emotable.setId(id);
            emotable.setCreationTime(2);
            emotable.setMaxInactiveInterval(4);
            Immoter immoter=disc1.getDemoter(id, emotable);
            Emoter emoter=new EtherEmoter();
            Utils.mote(emoter, immoter, emotable, id);
            assertTrue(d1.containsKey(id));
            assertTrue(d1.size()==1);
        }
        {
            // place a "foo" item into memory
            String id="foo";
            Motable emotable=_distributableSessionPool.take();
            emotable.setId(id);
            emotable.setCreationTime(1);
            emotable.setMaxInactiveInterval(2);
            m.put(id, emotable);
            assertTrue(m.containsKey(id));
            assertTrue(m.size()==1);
        }
        
        // ensure that all 3 sessions are promoted to memory...
        FilterChain fc=new MyFilterChain();
        memory.contextualise(_request,null,fc,"foo", null, null, false);
        memory.contextualise(null,null,fc,"bar", null, null, false);
        memory.contextualise(null,null,fc,"baz", null, null, false);
        assertTrue(d2.size()==0);
        assertTrue(d1.size()==0);
        assertTrue(m.size()==3);
        
        // check their content...
        Session baz=(Session)m.get("baz");
        assertTrue(baz!=null);
        assertTrue("baz".equals(baz.getId()));
        assertTrue(baz.getCreationTime()==3);
        assertTrue(baz.getMaxInactiveInterval()==6);
        
        Session bar=(Session)m.get("bar");
        assertTrue(bar!=null);
        assertTrue("bar".equals(bar.getId()));
        assertTrue(bar.getCreationTime()==2);
        assertTrue(bar.getMaxInactiveInterval()==4);
        
        Session foo=(Session)m.get("foo");
        assertTrue(foo!=null);
        assertTrue("foo".equals(foo.getId()));
        assertTrue(foo.getCreationTime()==1);
        assertTrue(foo.getMaxInactiveInterval()==2);
    }
    
    public void testSharedPromotion() throws Exception {
        SharedJDBCContextualiser db=new SharedJDBCContextualiser(_dummyContextualiser, _collapser, _ds, _table);
        Map m=new HashMap();
        Contextualiser serial=new SerialContextualiser(db, _collapser, m);
        Contextualiser memory=new MemoryContextualiser(serial, _dummyEvicter, m, _streamer, _distributableContextPool, _requestPool);
        Manager manager=new DistributableManager(_distributableSessionPool, _distributableAttributesPool, _distributableValuePool, _sessionWrapperFactory, _sessionIdFactory, memory, m, _router, _streamer, _accessOnLoad);
        
        {
            // place a "foo" item into shared database
            String id="foo";
            Motable emotable=_distributableSessionPool.take();
            emotable.setId(id);
            emotable.setCreationTime(2);
            emotable.setMaxInactiveInterval(4);
            Immoter immoter=db.getDemoter(id, emotable);
            Emoter emoter=new EtherEmoter();
            Utils.mote(emoter, immoter, emotable, id);
        }
        
        FilterChain fc=new MyFilterChain();
        
        memory.contextualise(null,null,fc,"foo", null, null, false);
        assertTrue(m.size()==0); // this should not go to the db...
        
        manager.start(); // this should promote all shared sessions into exclusively owned space (i.e. memory)
        
        memory.contextualise(null,null,fc,"foo", null, null, false);
        assertTrue(m.size()==1); // foo should be here now...
        
        // check it's content
        Session foo=(Session)m.get("foo");
        assertTrue(foo!=null);
        assertTrue("foo".equals(foo.getId()));
        assertTrue(foo.getCreationTime()==2);
        assertTrue(foo.getMaxInactiveInterval()==4);
    }
    
    class MyPromotingContextualiser extends AbstractContextualiser {
        int _counter=0;
        MyContext _context;
        
        public MyPromotingContextualiser(String context) {
            _context=new MyContext(context, context);
        }
        
        public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws IOException, ServletException {
            _counter++;
            
            Motable emotable=_context;
            Emoter emoter=new EtherEmoter();
            Motable immotable=Utils.mote(emoter, immoter, emotable, id);
            if (immotable!=null) {
                return immoter.contextualise(hreq, hres, chain, id, immotable, motionLock);
            } else {
                return false;
            }
        }
        
        public Evicter getEvicter(){return null;}
        
        public boolean isExclusive(){return false;}
        
        public Immoter getDemoter(String id, Motable motable) {
            return null;
        }
        
        public Immoter getSharedDemoter(){throw new UnsupportedOperationException();}
        
        public void promoteToExclusive(Immoter immoter){/* empty */}
        public int loadMotables(Emoter emoter, Immoter immoter) {return 0;}
        
        public void setLastAccessedTime(Evictable evictable, long oldTime, long newTime){/* empty */}
        public void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval) {/* do nothing */}
        
    }
    
    class MyActiveContextualiser extends AbstractContextualiser {
        int _counter=0;
        MyContext _context;
        
        public MyActiveContextualiser(String context) {
            _context=new MyContext(context, context);
        }
        
        public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws IOException, ServletException {
            _counter++;
            Context context=_context;
            Sync shared=context.getSharedLock();
            try {
                shared.acquire();
                motionLock.release();
                _log.info("running locally: "+id);
                chain.doFilter(hreq, hres);
                shared.release();
                return true;
            } catch (InterruptedException e) {
                throw new ServletException("problem processing request for: "+id, e);
            }
        }
        
        public Evicter getEvicter(){return null;}
        
        public boolean isExclusive(){return false;}
        
        public Immoter getDemoter(String id, Motable motable) {
            return null;
        }
        
        public Immoter getSharedDemoter(){throw new UnsupportedOperationException();}
        
        public void promoteToExclusive(Immoter immoter){/* empty */}
        public int loadMotables(Emoter emoter, Immoter immoter) {return 0;}
        
        public void setLastAccessedTime(Evictable evictable, long oldTime, long newTime){/* empty */}
        public void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval) {/* do nothing */}
        
    }
    
    class MyRunnable implements Runnable {
        Contextualiser _contextualiser;
        FilterChain    _chain;
        String         _id;
        
        MyRunnable(Contextualiser contextualiser, FilterChain chain, String id) {
            _contextualiser=contextualiser;
            _chain=chain;
            _id=id;
        }
        
        public void run() {
            try {
                _contextualiser.contextualise(null, null, _chain, _id, null, null, false);
            } catch (Exception e) {
                _log.error("unexpected problem", e);
                assertTrue(false);
            }
        }
    }
    
    public void testPromotion(Contextualiser c, int n) throws Exception {
        Contextualiser mc=new MemoryContextualiser(c, new DummyEvicter(), new HashMap(), new GZIPStreamer(), new MyContextPool(), _requestPool);
        FilterChain fc=new MyFilterChain();
        
        for (int i=0; i<n; i++)
            mc.contextualise(null,null,fc,"baz", null, new NullSync(), false);
    }
    
    public void testPromotion() throws Exception {
        int n=10;
        MyPromotingContextualiser mpc=new MyPromotingContextualiser("baz");
        testPromotion(mpc, n);
        assertTrue(mpc._counter==1);
        
        MyActiveContextualiser mac=new MyActiveContextualiser("baz");
        testPromotion(mac, n);
        assertTrue(mac._counter==n);
    }
    
    public void testCollapsing(Contextualiser c, int n) throws Exception {
        Map map=new HashMap();
        Contextualiser sc=new SerialContextualiser(c, new HashingCollapser(1, 1000), map);
        Contextualiser mc=new MemoryContextualiser(sc, new DummyEvicter(), map, new GZIPStreamer(), new MyContextPool(), _requestPool);
        FilterChain fc=new MyFilterChain();
        
        Runnable r=new MyRunnable(mc, fc, "baz");
        Thread[] threads=new Thread[n];
        for (int i=0; i<n; i++)
            (threads[i]=new Thread(r)).start();
        for (int i=0; i<n; i++)
            threads[i].join();
    }
    
    public void testCollapsing() throws Exception {
        int n=10;
        MyPromotingContextualiser mpc=new MyPromotingContextualiser("baz");
        testCollapsing(mpc, n);
        assertTrue(mpc._counter==1);
        
        MyActiveContextualiser mxc=new MyActiveContextualiser("baz");
        testCollapsing(mxc, n);
        assertTrue(mxc._counter==n);
    }
    
    // reusable components...
    // shared
    protected final Streamer _streamer=new SimpleStreamer();
    protected final Contextualiser _dummyContextualiser=new DummyContextualiser();
    protected final File _dir=new File("/tmp");
    protected final Collapser _collapser=new HashingCollapser(1, 2000);
    protected final SessionWrapperFactory _sessionWrapperFactory=new JettySessionWrapperFactory();
    protected final SessionIdFactory _sessionIdFactory=new TomcatSessionIdFactory();
    protected final boolean _accessOnLoad=true;
    protected final Router _router=new DummyRouter();
    
    // standard
    protected final SessionPool _standardSessionPool=new SimpleSessionPool(new StandardSessionFactory()); 
    protected final ContextPool _standardContextPool=new SessionToContextPoolAdapter(_standardSessionPool); 
    protected final AttributesPool _standardAttributesPool=new SimpleAttributesPool(new StandardAttributesFactory());
    protected final ValuePool _standardValuePool=new SimpleValuePool(new StandardValueFactory());
    
    // distributable
    protected final SessionPool _distributableSessionPool=new SimpleSessionPool(new DistributableSessionFactory()); 
    protected final ContextPool _distributableContextPool=new SessionToContextPoolAdapter(_distributableSessionPool); 
    protected final AttributesPool _distributableAttributesPool=new SimpleAttributesPool(new DistributableAttributesFactory());
    protected final ValuePool _distributableValuePool=new SimpleValuePool(new DistributableValueFactory());
    
    public void testExpiry() throws Exception {
        Map m=new HashMap();
        Evicter memoryEvicter=new NeverEvicter(30, true);
        MemoryContextualiser memory=new MemoryContextualiser(_dummyContextualiser, memoryEvicter, m, _streamer, _standardContextPool, _requestPool);
        Manager manager=new Manager(_standardSessionPool, _standardAttributesPool, _standardValuePool, _sessionWrapperFactory, _sessionIdFactory, memory, m, _router, _accessOnLoad);
        Session session=manager.createSession();
        session.setMaxInactiveInterval(1);
        assertTrue(m.size()==1); // in memory
        memoryEvicter.evict();
        assertTrue(m.size()==1); // still in memory
        Thread.sleep(1000);
        memoryEvicter.evict();
        assertTrue(m.size()==0); // no longer in memory - expired
    }
    
    public void testDemotionAndExpiry() throws Exception {
        Map d=new HashMap();
        Evicter discEvicter=new NeverEvicter(30, true);
        Contextualiser disc=new ExclusiveDiscContextualiser(_dummyContextualiser, _collapser, discEvicter, d, _streamer, _dir);
        Map m=new HashMap();
        Contextualiser serial=new SerialContextualiser(disc, _collapser, m);
        Evicter memoryEvicter=new AbsoluteEvicter(30, true, 1);
        Contextualiser memory=new MemoryContextualiser(serial, memoryEvicter, m, _streamer, _distributableContextPool, _requestPool);
        Manager manager=new DistributableManager(_distributableSessionPool, _distributableAttributesPool, _distributableValuePool, _sessionWrapperFactory, _sessionIdFactory, memory, m, _router, _streamer, _accessOnLoad);
        
        Session session=manager.createSession();
        session.setMaxInactiveInterval(2);// times out 2 seconds from now...
        
        assertTrue(m.size()==1); // in memory
        assertTrue(d.size()==0); // not on disc
        
        memoryEvicter.evict();
        assertTrue(m.size()==1); // still in memory
        assertTrue(d.size()==0); // still not on disc
        
        Thread.sleep(1000);
        memoryEvicter.evict();
        assertTrue(m.size()==0); // no longer in memory
        assertTrue(d.size()==1); // now on disc
        
        discEvicter.evict();
        assertTrue(m.size()==0); // no longer in memory
        assertTrue(d.size()==1); // still on disc
        
        Thread.sleep(1000);
        discEvicter.evict();
        assertTrue(m.size()==0); // no longer in memory
        assertTrue(d.size()==0); // no longer on disc - expired
    }
    
    public void testDemotionAndPromotion() throws Exception {
        Map d=new HashMap();
        Evicter discEvicter=new NeverEvicter(30, true);
        Contextualiser disc=new ExclusiveDiscContextualiser(_dummyContextualiser, _collapser, discEvicter, d, _streamer, _dir);
        Map m=new HashMap();
        Contextualiser serial=new SerialContextualiser(disc, _collapser, m);
        Evicter memoryEvicter=new AbsoluteEvicter(30, true, 1);
        Contextualiser memory=new MemoryContextualiser(serial, memoryEvicter, m, _streamer, _distributableContextPool, _requestPool);
        Manager manager=new DistributableManager(_distributableSessionPool, _distributableAttributesPool, _distributableValuePool, _sessionWrapperFactory, _sessionIdFactory, memory, m, _router, _streamer, _accessOnLoad);
        
        Session session=manager.createSession();
        String id=session.getId();
        session.setMaxInactiveInterval(2);// times out 2 seconds from now...
        
        Thread.sleep(1000);
        memoryEvicter.evict();
        assertTrue(m.size()==0); // no longer in memory
        assertTrue(d.size()==1); // now on disc
        
        memory.contextualise(null,null,new MyFilterChain(),id, null, null, false);
        assertTrue(m.size()==1); // promoted back into memory
        assertTrue(d.size()==0); // no longer on disc
    }
    
    static class MyLocation extends SimpleEvictable implements Location, Serializable {
        
        public void proxy(HttpServletRequest hreq, HttpServletResponse hres) {
            System.out.println("PROXYING");
        }
        
        public Destination getDestination(){return null;}
    }
    
    public void testCluster() throws Exception {
//      ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://WADI-TEST");
        ConnectionFactory connectionFactory = Utils.getConnectionFactory();
        ((ActiveMQConnectionFactory)connectionFactory).setBrokerContainerFactory(new BrokerContainerFactoryImpl(new VMPersistenceAdapter()));
        ClusterFactory clusterFactory       = new CustomClusterFactory(connectionFactory);
        String clusterName                  = "ORG.CODEHAUS.WADI.TEST.CLUSTER";
        CustomCluster cluster0              = (CustomCluster)clusterFactory.createCluster(clusterName);
        CustomCluster cluster1              = (CustomCluster)clusterFactory.createCluster(clusterName);
        
        cluster0.start();
        cluster1.start();
        //-------------------
        // do the test
        
        Location location=new MyLocation();
        Map c0=new HashMap();
        MessageDispatcher dispatcher0=new MessageDispatcher(cluster0);
        RelocationStrategy relocater0=new ProxyRelocationStrategy(dispatcher0, location, 2000, 3000);
        Collapser collapser0=new HashingCollapser(10, 2000);
        ClusterContextualiser clstr0=new ClusterContextualiser(new DummyContextualiser(), collapser0, new SwitchableEvicter(30000, true), c0, cluster0, dispatcher0, relocater0, location);
        Map m0=new HashMap();
        m0.put("foo", new MyContext());
        Contextualiser memory0=new MemoryContextualiser(clstr0, new NeverEvicter(30000, true), m0, new GZIPStreamer(), new MyContextPool(), _requestPool);
        relocater0.setTop(memory0);
        
        Map c1=new HashMap();
        MessageDispatcher dispatcher1=new MessageDispatcher(cluster1);
        RelocationStrategy relocater1=new ProxyRelocationStrategy(dispatcher1, location, 2000, 3000);
        Collapser collapser1=new HashingCollapser(10, 2000);
        ClusterContextualiser clstr1=new ClusterContextualiser(new DummyContextualiser(), collapser1, new SwitchableEvicter(30000, true), c1, cluster1, dispatcher1, relocater1, null);
        Map m1=new HashMap();
        m1.put("bar", new MyContext());
        Contextualiser memory1=new MemoryContextualiser(clstr1, new NeverEvicter(30000, true), m1, new GZIPStreamer(), new MyContextPool(), _requestPool);
        relocater1.setTop(memory1);
        
        Thread.sleep(2000); // activecluster needs a little time to sort itself out...
        _log.info("STARTING NOW!");
        FilterChain fc=new MyFilterChain();
        
        assertTrue(!m0.containsKey("bar"));
        assertTrue(!m1.containsKey("foo"));
        // not sure what these were testing - if Context not available, these will return false...
//      assertTrue(memory0.contextualise(null,null,fc,"bar", null, null, false));
//      assertTrue(memory0.contextualise(null,null,fc,"bar", null, null, false));
//      assertTrue(memory1.contextualise(null,null,fc,"foo", null, null, false));
//      assertTrue(memory1.contextualise(null,null,fc,"foo", null, null, false));
        assertTrue(!memory0.contextualise(null,null,fc,"baz", null, null, false));
        assertTrue(!memory1.contextualise(null,null,fc,"baz", null, null, false));
        
        Thread.sleep(2000);
        _log.info("STOPPING NOW!");
        // ------------------
        cluster1.stop();
        cluster1=null;
        cluster0.stop();
        cluster0=null;
        clusterFactory=null;
        connectionFactory=null;
    }
    
    // TODO - add some content to this test...
    public void testStack() throws Exception {
        _log.info("putting complete stack together...");
        Map map=new ConcurrentHashMap();
        SimpleContextualiserStack stack=new SimpleContextualiserStack(map, _standardContextPool, _ds, 8080);
        Manager manager=new Manager(_standardSessionPool, _standardAttributesPool, _standardValuePool, _sessionWrapperFactory, _sessionIdFactory, stack, map, _router, _accessOnLoad);
        manager.start();
        Thread.sleep(2000);
        stack.stop();
        Thread.sleep(2000);
        _log.info("...done");
    }
    
}
