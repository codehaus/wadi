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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.Statement;
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
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.ContextPool;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Location;
import org.codehaus.wadi.sandbox.context.Motable;
import org.codehaus.wadi.sandbox.context.Promoter;
import org.codehaus.wadi.sandbox.context.ProxyingException;
import org.codehaus.wadi.sandbox.context.RelocationStrategy;
import org.codehaus.wadi.sandbox.context.impl.AlwaysEvicter;
import org.codehaus.wadi.sandbox.context.impl.ProxyRelocationStrategy;
import org.codehaus.wadi.sandbox.context.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.context.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.context.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.context.impl.LocalDiscContextualiser;
import org.codehaus.wadi.sandbox.context.impl.MemoryContextualiser;
import org.codehaus.wadi.sandbox.context.impl.MessageDispatcher;
import org.codehaus.wadi.sandbox.context.impl.NeverEvicter;
import org.codehaus.wadi.sandbox.context.impl.SharedJDBCContextualiser;

import EDU.oswego.cs.dl.util.concurrent.Sync;

import junit.framework.TestCase;

/**
 * TODO - JavaDoc this type
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

		Connection c=_ds.getConnection();
		Statement s=c.createStatement();
		// TODO - should parameterise the column names when code stabilises...
		s.execute("create table "+_table+"(MyKey varchar, MyValue java_object)");
		s.close();
		c.close();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
			Connection c=_ds.getConnection();
			Statement s=c.createStatement();
			s.execute("drop table "+_table);
			s.execute("SHUTDOWN");
			s.close();
			c.close();
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
		Map d=new HashMap();
		StreamingStrategy ss=new SimpleStreamingStrategy();
		File f=File.createTempFile("wadi.", "."+ss.getSuffix());
		_log.info("file: "+f);
	    ObjectOutput oo=ss.getOutputStream(new FileOutputStream(f));
	    new MyContext("bar").writeContent(oo);
	    oo.flush();
	    oo.close();
	    assertTrue(f.exists());
		d.put("bar", new LocalDiscContextualiser.LocalDiscMotable(0, f));
		Contextualiser disc=new LocalDiscContextualiser(new DummyContextualiser(), collapser, d, new NeverEvicter(), ss, new File("/tmp"));
		Map m=new HashMap();
		m.put("foo", new MyContext("foo"));
		Contextualiser memory=new MemoryContextualiser(disc, collapser, m, new NeverEvicter(), new GZIPStreamingStrategy(), new MyContextPool());

		FilterChain fc=new MyFilterChain();
//		Collapser collapser=new HashingCollapser();
		memory.contextualise(null,null,fc,"foo", null, null, false);
		memory.contextualise(null,null,fc,"bar", null, null, false);
		assertTrue(!f.exists());
	}

	class MyPromotingContextualiser implements Contextualiser {
		int _counter=0;
		MyContext _context;

		public MyPromotingContextualiser(String context) {
			_context=new MyContext(context);
		}

		public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Promoter promoter, Sync promotionLock, boolean localOnly) throws IOException, ServletException {
			_counter++;
			
			try {
				Motable p=promoter.nextMotable();
				assertTrue(_context!=null);
				try {
					_context.setBytes(p.getBytes());
				} catch (ClassNotFoundException e) {
					_log.warn("could not promote context", e);
				}
				if (promoter.prepare(id, p)) {
					_context=null;
					promoter.commit(id, p);
					promotionLock.release();
					promoter.contextualise(hreq, hres, chain, id, p);
					return true;
				} else {
					promoter.rollback(id, p);
					return false;
				}
			} catch (Exception e) {
				_log.warn("unexpected problem", e);
				return false;
			}
		}

		public void demote(String key, Motable val){}
		public void evict(){}

		public boolean isLocal(){return false;}
	}

	class MyActiveContextualiser implements Contextualiser {
		int _counter=0;
		MyContext _context;

		public MyActiveContextualiser(String context) {
			_context=new MyContext(context);
		}

		public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Promoter promoter, Sync promotionLock, boolean localOnly) throws IOException, ServletException {
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

		public void demote(String key, Motable val){}
		public void evict(){}
		public boolean isLocal(){return false;}
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
		Contextualiser mc=new MemoryContextualiser(c, new HashingCollapser(10, 2000), new HashMap(), new NeverEvicter(), new GZIPStreamingStrategy(), new MyContextPool());
		FilterChain fc=new MyFilterChain();

		for (int i=0; i<n; i++)
			mc.contextualise(null,null,fc,"baz", null, null, false);
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
		Contextualiser mc=new MemoryContextualiser(c, new HashingCollapser(10, 2000), new HashMap(), new NeverEvicter(), new GZIPStreamingStrategy(), new MyContextPool());
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
		Contextualiser disc=new LocalDiscContextualiser(new DummyContextualiser(), collapser, d, new NeverEvicter(), ss, new File("/tmp"));
		Map m=new HashMap();
		m.put("foo", new MyContext("foo"));
		Contextualiser memory=new MemoryContextualiser(disc, collapser, m, new AlwaysEvicter(), new GZIPStreamingStrategy(), new MyContextPool());

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
	public void testEviction3() throws Exception {
		Collapser collapser=new HashingCollapser(10, 2000);
		StreamingStrategy ss=new SimpleStreamingStrategy();
		Contextualiser db=new SharedJDBCContextualiser(new DummyContextualiser(), collapser, new NeverEvicter(), ss, _ds, _table);
		Map d=new HashMap();
		Contextualiser disc=new LocalDiscContextualiser(db, collapser, d, new MyEvicter(0), ss, new File("/tmp"));
		Map m=new HashMap();
		m.put("foo", new MyContext("foo", System.currentTimeMillis()+2000)); // times out 2 seconds from now...
		Contextualiser memory=new MemoryContextualiser(disc, collapser, m, new MyEvicter(1000), new GZIPStreamingStrategy(), new MyContextPool());

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

	static class MyLocation implements Location, Serializable {
		public long getExpiryTime(){return 0;}
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
		ClusterContextualiser clstr0=new ClusterContextualiser(new DummyContextualiser(), collapser0, c0, new MyEvicter(0), dispatcher0, relocater0);
		Map m0=new HashMap();
		m0.put("foo", new MyContext());
		Contextualiser memory0=new MemoryContextualiser(clstr0, collapser0, m0, new NeverEvicter(), new GZIPStreamingStrategy(), new MyContextPool());
		relocater0.setTop(memory0);

		Collapser collapser1=new HashingCollapser(10, 2000);
		Map c1=new HashMap();
		MessageDispatcher dispatcher1=new MessageDispatcher(cluster1);
		RelocationStrategy relocater1=new ProxyRelocationStrategy(dispatcher1, location, 2000, 3000);
		ClusterContextualiser clstr1=new ClusterContextualiser(new DummyContextualiser(), collapser1, c1, new MyEvicter(0), dispatcher1, relocater1);
		Map m1=new HashMap();
		m1.put("bar", new MyContext());
		Contextualiser memory1=new MemoryContextualiser(clstr1, collapser1, m1, new NeverEvicter(), new GZIPStreamingStrategy(), new MyContextPool());
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
