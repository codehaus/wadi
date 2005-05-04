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
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.GZIPStreamingStrategy;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.Context;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Evictable;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.HttpServletRequestWrapperPool;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Location;
import org.codehaus.wadi.sandbox.Motable;
import org.codehaus.wadi.sandbox.RelocationStrategy;
import org.codehaus.wadi.sandbox.impl.AbsoluteEvicter;
import org.codehaus.wadi.sandbox.impl.AbstractContextualiser;
import org.codehaus.wadi.sandbox.impl.AlwaysEvicter;
import org.codehaus.wadi.sandbox.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.impl.CustomCluster;
import org.codehaus.wadi.sandbox.impl.CustomClusterFactory;
import org.codehaus.wadi.sandbox.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.impl.DummyEvicter;
import org.codehaus.wadi.sandbox.impl.DummyHttpServletRequest;
import org.codehaus.wadi.sandbox.impl.ExclusiveDiscContextualiser;
import org.codehaus.wadi.sandbox.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.impl.MemoryContextualiser;
import org.codehaus.wadi.sandbox.impl.MessageDispatcher;
import org.codehaus.wadi.sandbox.impl.NeverEvicter;
import org.codehaus.wadi.sandbox.impl.ProxyRelocationStrategy;
import org.codehaus.wadi.sandbox.impl.SerialContextualiser;
import org.codehaus.wadi.sandbox.impl.SharedJDBCContextualiser;
import org.codehaus.wadi.sandbox.impl.SharedJDBCMotable;
import org.codehaus.wadi.sandbox.impl.SimpleContextualiserStack;
import org.codehaus.wadi.sandbox.impl.SimpleEvictable;
import org.codehaus.wadi.sandbox.impl.TimeToLiveEvicter;
import org.codehaus.wadi.sandbox.impl.Utils;

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

	public void testConctextualiser() throws Exception {
		Collapser collapser=new HashingCollapser(10, 2000);
		StreamingStrategy ss=new SimpleStreamingStrategy();

		SharedJDBCContextualiser db=new SharedJDBCContextualiser(new DummyContextualiser(), collapser, _ds, _table);
		{
			// place a "baz" item into database
			String id="baz";
			Motable emotable=new MyContext(id, id);
			emotable.setCreationTime(3);
			emotable.setLastAccessedTime(4);
			emotable.setMaxInactiveInterval(5);
			Immoter immoter=db.getDemoter(id, emotable);
			Emoter emoter=new EtherEmoter();
			Utils.mote(emoter, immoter, emotable, id);
		}

		Map d=new HashMap();
		ExclusiveDiscContextualiser disc=new ExclusiveDiscContextualiser(db, collapser, new DummyEvicter(), d, ss, new File("/tmp"));
		{
			// place a "bar" item onto local disc
			String id="bar";
			Motable emotable=new MyContext(id, id);
			emotable.setCreationTime(0);
			emotable.setLastAccessedTime(1);
			emotable.setMaxInactiveInterval(2);
			Immoter immoter=disc.getDemoter(id, emotable);
			Emoter emoter=new EtherEmoter();
			Utils.mote(emoter, immoter, emotable, id);
		}
		assertTrue(d.containsKey("bar"));
		assertTrue(d.size()==1);

        Map m=new HashMap();
		Contextualiser serial=new SerialContextualiser(disc, collapser, m);

		Contextualiser memory=new MemoryContextualiser(serial, new DummyEvicter(), m, ss, new MyContextPool(), _requestPool);
        
        memory.init(new DummyContextualiserConfig(memory, m));
        
		m.put("foo", new MyContext("foo", "foo"));
		assertTrue(m.size()==1);

		FilterChain fc=new MyFilterChain();
		memory.contextualise(_request,null,fc,"foo", null, null, false);
		memory.contextualise(null,null,fc,"bar", null, null, false);
		memory.contextualise(null,null,fc,"baz", null, null, false);
		assertTrue(d.size()==0);
		assertTrue(m.size()==3);

		MyContext bar=(MyContext)m.get("bar");
		assertTrue(bar!=null);
		assertTrue("bar".equals(bar._val));
		assertTrue(bar.getCreationTime()==0);
		// assertTrue(bar.getLastAccessedTime()==1); // this is now set when contextualising...
		assertTrue(bar.getMaxInactiveInterval()==2);

		MyContext baz=(MyContext)m.get("baz");
		assertTrue(baz!=null);
		assertTrue("baz".equals(baz._val));
		assertTrue(baz.getCreationTime()==3);
		// assertTrue(baz.getLastAccessedTime()==4); // this is now set when contextualising...
		assertTrue(baz.getMaxInactiveInterval()==5);
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
		Contextualiser mc=new MemoryContextualiser(c, new DummyEvicter(), new HashMap(), new GZIPStreamingStrategy(), new MyContextPool(), _requestPool);
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
		Contextualiser mc=new MemoryContextualiser(sc, new DummyEvicter(), map, new GZIPStreamingStrategy(), new MyContextPool(), _requestPool);
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

    public void testEviction2() throws Exception {
		Collapser collapser=new HashingCollapser(10, 2000);
		StreamingStrategy ss=new SimpleStreamingStrategy();
		Map d=new HashMap();
		Contextualiser disc=new ExclusiveDiscContextualiser(new DummyContextualiser(), collapser, new DummyEvicter(), d, ss, new File("/tmp"));
        Map m=new HashMap();
		Contextualiser serial=new SerialContextualiser(disc, collapser, m);
		Evictable foo=new MyContext("foo", "foo");
		foo.setMaxInactiveInterval(30*60*60);
		m.put("foo", foo);
        Evicter memoryEvicter=new AlwaysEvicter(30000, true);
		Contextualiser memory=new MemoryContextualiser(serial, memoryEvicter, m, new GZIPStreamingStrategy(), new MyContextPool(), _requestPool);
        memory.init(new DummyContextualiserConfig(memory, m));

		FilterChain fc=new MyFilterChain();

		assertTrue(!d.containsKey("foo"));
		assertTrue(m.containsKey("foo"));
		memoryEvicter.evict(); // should move foo to disc
		assertTrue(d.containsKey("foo"));
		assertTrue(!m.containsKey("foo"));
		memory.contextualise(null,null,fc,"foo", null, null, false); // should promote foo back into memory
		assertTrue(!d.containsKey("foo"));
		assertTrue(m.containsKey("foo"));
	}

	public void testEviction3() throws Exception {
		Collapser collapser=new HashingCollapser(10, 2000);
		StreamingStrategy ss=new SimpleStreamingStrategy();
		Contextualiser db=new SharedJDBCContextualiser(new DummyContextualiser(), null, _ds, _table);
		Map d=new HashMap();
        Evicter discEvicter=new TimeToLiveEvicter(30000, true, 0);
		Contextualiser disc=new ExclusiveDiscContextualiser(db, collapser, discEvicter, d, ss, new File("/tmp"));
        Map m=new HashMap();
		Contextualiser serial=new SerialContextualiser(disc, collapser, m);
		Context tmp=new MyContext("foo", "foo");
		tmp.setMaxInactiveInterval(2);
		m.put("foo", tmp); // times out 2 seconds from now...
        Evicter memoryEvicter=new TimeToLiveEvicter(30000, true, 1000);
		Contextualiser memory=new MemoryContextualiser(serial, memoryEvicter, m, new GZIPStreamingStrategy(), new MyContextPool(), _requestPool);
        memory.init(new DummyContextualiserConfig(memory, m));

		FilterChain fc=new MyFilterChain();

		assertTrue(!d.containsKey("foo"));
		assertTrue(m.containsKey("foo"));
		memoryEvicter.evict(); // should not have timed out yet
		assertTrue(!d.containsKey("foo"));
		assertTrue(m.containsKey("foo"));
		Thread.sleep(1000);
		memoryEvicter.evict(); // should now be a second old - moved to disc
		assertTrue(d.containsKey("foo"));
		assertTrue(!m.containsKey("foo"));
		discEvicter.evict(); // should stay on disc...
		assertTrue(d.containsKey("foo"));
		assertTrue(!m.containsKey("foo"));
		Thread.sleep(1000);
		discEvicter.evict(); // should have expired...
		assertTrue(!d.containsKey("foo"));
		assertTrue(!m.containsKey("foo"));
	}

	static class MyLocation extends SimpleEvictable implements Location, Serializable {

		public void proxy(HttpServletRequest hreq, HttpServletResponse hres) {
			System.out.println("PROXYING");
		}

		public Destination getDestination(){return null;}
	}

	public void testCluster() throws Exception {
//        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://WADI-TEST");
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
		Contextualiser memory0=new MemoryContextualiser(clstr0, new NeverEvicter(30000, true), m0, new GZIPStreamingStrategy(), new MyContextPool(), _requestPool);
		relocater0.setTop(memory0);

		Map c1=new HashMap();
		MessageDispatcher dispatcher1=new MessageDispatcher(cluster1);
		RelocationStrategy relocater1=new ProxyRelocationStrategy(dispatcher1, location, 2000, 3000);
		Collapser collapser1=new HashingCollapser(10, 2000);
		ClusterContextualiser clstr1=new ClusterContextualiser(new DummyContextualiser(), collapser1, new SwitchableEvicter(30000, true), c1, cluster1, dispatcher1, relocater1, null);
		Map m1=new HashMap();
		m1.put("bar", new MyContext());
		Contextualiser memory1=new MemoryContextualiser(clstr1, new NeverEvicter(30000, true), m1, new GZIPStreamingStrategy(), new MyContextPool(), _requestPool);
		relocater1.setTop(memory1);

	    Thread.sleep(2000); // activecluster needs a little time to sort itself out...
	    _log.info("STARTING NOW!");
		FilterChain fc=new MyFilterChain();

		assertTrue(!m0.containsKey("bar"));
		assertTrue(!m1.containsKey("foo"));
		// not sure what these were testing - if Context not available, these will return false...
//		assertTrue(memory0.contextualise(null,null,fc,"bar", null, null, false));
//		assertTrue(memory0.contextualise(null,null,fc,"bar", null, null, false));
//		assertTrue(memory1.contextualise(null,null,fc,"foo", null, null, false));
//		assertTrue(memory1.contextualise(null,null,fc,"foo", null, null, false));
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

	public void testTimeOut() throws Exception {
	    StreamingStrategy streamer=new SimpleStreamingStrategy();
	    Collapser collapser=new HashingCollapser(1, 2000);
		Map d=new HashMap();
		ExclusiveDiscContextualiser disc=new ExclusiveDiscContextualiser(new DummyContextualiser(), collapser, new NeverEvicter(30000, true), d, streamer, new File("/tmp"));
		Map m=new HashMap();
        Evicter memoryEvicter=new AbsoluteEvicter(30000, true, 30*60); // 30 mins
		MemoryContextualiser memory=new MemoryContextualiser(disc, memoryEvicter, m, streamer, new MyContextPool(), _requestPool);
        memory.init(new DummyContextualiserConfig(memory, m));
		Context foo=new MyContext("foo", "foo");
        foo.setMaxInactiveInterval(1);
		m.put("foo", foo);
		assertTrue(m.size()==1);
        assertTrue(d.size()==0);
        Thread.sleep(1000);
		memoryEvicter.evict();
		assertTrue(m.size()==0); // should not be in memory
		assertTrue(d.size()==0); // should not be on disc - should have fallen though - since invalidated
	}

    public void testStack() throws Exception {
        _log.info("putting complete stack together...");
        Map map=new ConcurrentHashMap();
        SimpleContextualiserStack stack=new SimpleContextualiserStack(map, new MyContextPool(), _ds, 8080);
        stack.init(new DummyContextualiserConfig(stack.getTop(), map)); // clumsy
        stack.start();
        Thread.sleep(2000);
        stack.stop();
        Thread.sleep(2000);
        _log.info("...done");
    }
}
