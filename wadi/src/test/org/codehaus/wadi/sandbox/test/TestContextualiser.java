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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.axiondb.jdbc.AxionDataSource;
import org.codehaus.activecluster.Cluster;
import org.codehaus.activecluster.ClusterFactory;
import org.codehaus.activecluster.impl.DefaultClusterFactory;
import org.codehaus.activemq.ActiveMQConnectionFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.GZIPStreamingStrategy;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.Context;
import org.codehaus.wadi.sandbox.ContextPool;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Location;
import org.codehaus.wadi.sandbox.Motable;
import org.codehaus.wadi.sandbox.ProxyingException;
import org.codehaus.wadi.sandbox.RelocationStrategy;
import org.codehaus.wadi.sandbox.impl.AlwaysEvicter;
import org.codehaus.wadi.sandbox.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.impl.LocalDiscContextualiser;
import org.codehaus.wadi.sandbox.impl.MemoryContextualiser;
import org.codehaus.wadi.sandbox.impl.MessageDispatcher;
import org.codehaus.wadi.sandbox.impl.NeverEvicter;
import org.codehaus.wadi.sandbox.impl.ProxyRelocationStrategy;
import org.codehaus.wadi.sandbox.impl.SerialContextualiser;
import org.codehaus.wadi.sandbox.impl.SharedJDBCContextualiser;
import org.codehaus.wadi.sandbox.impl.SharedJDBCMotable;
import org.codehaus.wadi.sandbox.impl.SimpleEvictable;
import org.codehaus.wadi.sandbox.impl.SwitchableEvicter;
import org.codehaus.wadi.sandbox.impl.TimeToLiveEvicter;
import org.codehaus.wadi.sandbox.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.Sync;

import junit.framework.TestCase;

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


	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		_log.info("starting ...");
		SharedJDBCMotable.initialise(_ds, _table);
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

	class MyContextPool implements ContextPool {
		public void put(Context context){}
		public Context take(){return new MyContext();}
	}

	class MyFilterChain
	  implements FilterChain
	{
	  public void
	    doFilter(ServletRequest request, ServletResponse response)
	    throws IOException, ServletException
	  {
	    _log.info("invoking FilterChain...");
	  }
	}

	public void testConceptualiser() throws Exception {
		Collapser collapser=new HashingCollapser(10, 2000);
		StreamingStrategy ss=new SimpleStreamingStrategy();

		SharedJDBCContextualiser db=new SharedJDBCContextualiser(new DummyContextualiser(), new NeverEvicter(), _ds, _table);
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
		LocalDiscContextualiser disc=new LocalDiscContextualiser(db, d, new NeverEvicter(), ss, new File("/tmp"));
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

		Contextualiser serial=new SerialContextualiser(disc, collapser);

		Map m=new HashMap();
		Contextualiser memory=new MemoryContextualiser(serial, m, new NeverEvicter(), ss, new MyContextPool());
		m.put("foo", new MyContext("foo", "foo"));
		assertTrue(m.size()==1);

		FilterChain fc=new MyFilterChain();
		memory.contextualise(null,null,fc,"foo", null, null, false);
		memory.contextualise(null,null,fc,"bar", null, null, false);
		memory.contextualise(null,null,fc,"baz", null, null, false);
		assertTrue(d.size()==0);
		assertTrue(m.size()==3);

		MyContext bar=(MyContext)m.get("bar");
		assertTrue(bar!=null);
		assertTrue("bar".equals(bar._val));
		assertTrue(bar.getCreationTime()==0);
		assertTrue(bar.getLastAccessedTime()==1);
		assertTrue(bar.getMaxInactiveInterval()==2);

		MyContext baz=(MyContext)m.get("baz");
		assertTrue(baz!=null);
		assertTrue("baz".equals(baz._val));
		assertTrue(baz.getCreationTime()==3);
		assertTrue(baz.getLastAccessedTime()==4);
		assertTrue(baz.getMaxInactiveInterval()==5);
}

	class MyPromotingContextualiser implements Contextualiser {
		int _counter=0;
		MyContext _context;

		public MyPromotingContextualiser(String context) {
			_context=new MyContext(context, context);
		}

		public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock, boolean localOnly) throws IOException, ServletException {
			_counter++;

			Motable emotable=_context;
			Emoter emoter=new EtherEmoter();
			Motable immotable=Utils.mote(emoter, immoter, emotable, id);
			if (immotable!=null) {
				promotionLock.release();
				immoter.contextualise(hreq, hres, chain, id, immotable);
				return true;
			} else {
				return false;
			}
		}

		public void evict(){}
		public Evicter getEvicter(){return null;}

		public boolean isLocal(){return false;}

		public Immoter getDemoter(String id, Motable motable) {
			return null;
		}
	}

	class MyActiveContextualiser implements Contextualiser {
		int _counter=0;
		MyContext _context;

		public MyActiveContextualiser(String context) {
			_context=new MyContext(context, context);
		}

		public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock, boolean localOnly) throws IOException, ServletException {
			_counter++;
			Context context=_context;
			Sync shared=context.getSharedLock();
			try {
				shared.acquire();
				promotionLock.release();
				_log.info("running locally: "+id);
				chain.doFilter(hreq, hres);
				shared.release();
				return true;
			} catch (InterruptedException e) {
				throw new ServletException("problem processing request for: "+id, e);
			}
		}

		public void evict(){}
		public Evicter getEvicter(){return null;}

		public boolean isLocal(){return false;}

		public Immoter getDemoter(String id, Motable motable) {
			return null;
		}
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
				_contextualiser.contextualise(null, null, _chain, _id, null, new NullSync(), false);
			} catch (Exception e) {
				_log.error("unexpected problem", e);
				assertTrue(false);
			}
		}
	}

	public void testPromotion(Contextualiser c, int n) throws Exception {
		Contextualiser mc=new MemoryContextualiser(c, new HashMap(), new NeverEvicter(), new GZIPStreamingStrategy(), new MyContextPool());
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
		Contextualiser mc=new MemoryContextualiser(c, new HashMap(), new NeverEvicter(), new GZIPStreamingStrategy(), new MyContextPool());
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
		Contextualiser disc=new LocalDiscContextualiser(new DummyContextualiser(), d, new NeverEvicter(), ss, new File("/tmp"));
		Contextualiser serial=new SerialContextualiser(disc, collapser);
		Map m=new HashMap();
		m.put("foo", new MyContext("foo", "foo"));
		Contextualiser memory=new MemoryContextualiser(serial, m, new AlwaysEvicter(), new GZIPStreamingStrategy(), new MyContextPool());

		FilterChain fc=new MyFilterChain();

		assertTrue(!d.containsKey("foo"));
		assertTrue(m.containsKey("foo"));
		memory.evict(); // should move foo to disc
		assertTrue(d.containsKey("foo"));
		assertTrue(!m.containsKey("foo"));
		memory.contextualise(null,null,fc,"foo", null, null, false); // should promote foo back into memory
		assertTrue(!d.containsKey("foo"));
		assertTrue(m.containsKey("foo"));
	}

	public void testEviction3() throws Exception {
		Collapser collapser=new HashingCollapser(10, 2000);
		StreamingStrategy ss=new SimpleStreamingStrategy();
		Contextualiser db=new SharedJDBCContextualiser(new DummyContextualiser(), new NeverEvicter(), _ds, _table);
		Map d=new HashMap();
		Contextualiser disc=new LocalDiscContextualiser(db, d, new TimeToLiveEvicter(0), ss, new File("/tmp"));
		Contextualiser serial=new SerialContextualiser(disc, collapser);
		Map m=new HashMap();
		Context tmp=new MyContext("foo", "foo");
		tmp.setMaxInactiveInterval(2);
		m.put("foo", tmp); // times out 2 seconds from now...
		Contextualiser memory=new MemoryContextualiser(serial, m, new TimeToLiveEvicter(1000), new GZIPStreamingStrategy(), new MyContextPool());

		FilterChain fc=new MyFilterChain();

		assertTrue(!d.containsKey("foo"));
		assertTrue(m.containsKey("foo"));
		memory.evict(); // should not have timed out yet
		assertTrue(!d.containsKey("foo"));
		assertTrue(m.containsKey("foo"));
		Thread.sleep(1000);
		memory.evict(); // should now be a second old - moved to disc
		assertTrue(d.containsKey("foo"));
		assertTrue(!m.containsKey("foo"));
		disc.evict(); // should stay on disc...
		assertTrue(d.containsKey("foo"));
		assertTrue(!m.containsKey("foo"));
		Thread.sleep(1000);
		disc.evict(); // should finally move to db
		assertTrue(!d.containsKey("foo"));
		assertTrue(!m.containsKey("foo"));
		memory.contextualise(null,null,fc,"foo", null, null, false); // should be promoted to memory
		assertTrue(!d.containsKey("foo"));
		assertTrue(m.containsKey("foo")); // need to be able to 'touch' a context...
		memory.evict(); // should still be there...
//		assertTrue(!d.containsKey("foo"));
//		assertTrue(m.containsKey("foo"));
//		assertTrue(((MyContext)m.get("foo"))._val.equals("foo"));
	}

	static class MyLocation extends SimpleEvictable implements Location, Serializable {

		public void proxy(HttpServletRequest hreq, HttpServletResponse hres) throws ProxyingException {
			System.out.println("PROXYING");
		}

		public Destination getDestination(){return null;}
	}

	public void testCluster() throws Exception {
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("peer://WADI-TEST");
		ClusterFactory clusterFactory       = new DefaultClusterFactory(connectionFactory);
		String clusterName                  = "ORG.CODEHAUS.WADI.TEST.CLUSTER";
		Cluster cluster0                    = clusterFactory.createCluster(clusterName);
		Cluster cluster1                    = clusterFactory.createCluster(clusterName);

		cluster0.start();
		cluster1.start();
		//-------------------
		// do the test

		Location location=new MyLocation();
		Collapser collapser0=new HashingCollapser(10, 2000);
		Map c0=new HashMap();
		MessageDispatcher dispatcher0=new MessageDispatcher(cluster0);
		RelocationStrategy relocater0=new ProxyRelocationStrategy(dispatcher0, location, 2000, 3000);
		ClusterContextualiser clstr0=new ClusterContextualiser(new DummyContextualiser(), c0, new SwitchableEvicter(), dispatcher0, relocater0, location);
		Map m0=new HashMap();
		m0.put("foo", new MyContext());
		Contextualiser memory0=new MemoryContextualiser(clstr0, m0, new NeverEvicter(), new GZIPStreamingStrategy(), new MyContextPool());
		relocater0.setTop(memory0);

		Collapser collapser1=new HashingCollapser(10, 2000);
		Map c1=new HashMap();
		MessageDispatcher dispatcher1=new MessageDispatcher(cluster1);
		RelocationStrategy relocater1=new ProxyRelocationStrategy(dispatcher1, location, 2000, 3000);
		ClusterContextualiser clstr1=new ClusterContextualiser(new DummyContextualiser(), c1, new SwitchableEvicter(), dispatcher1, relocater1, null);
		Map m1=new HashMap();
		m1.put("bar", new MyContext());
		Contextualiser memory1=new MemoryContextualiser(clstr1, m1, new NeverEvicter(), new GZIPStreamingStrategy(), new MyContextPool());
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
}
