/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.axiondb.jdbc.AxionDataSource;
import org.codehaus.activecluster.Cluster;
import org.codehaus.activecluster.ClusterFactory;
import org.codehaus.activecluster.impl.DefaultClusterFactory;
import org.codehaus.activemq.ActiveMQConnectionFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.sandbox.context.impl.AlwaysEvicter;
import org.codehaus.wadi.sandbox.context.impl.ClusterContextualiser;
import org.codehaus.wadi.sandbox.context.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.context.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.context.impl.LocalDiscContextualiser;
import org.codehaus.wadi.sandbox.context.impl.LocationRequest;
import org.codehaus.wadi.sandbox.context.impl.LocationResponse;
import org.codehaus.wadi.sandbox.context.impl.MemoryContextualiser;
import org.codehaus.wadi.sandbox.context.impl.NeverEvicter;
import org.codehaus.wadi.sandbox.context.impl.SharedJDBCContextualiser;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

import junit.framework.TestCase;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
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
	
	class MyContext implements Context {
		String _val;
		ReadWriteLock _lock=new ReaderPreferenceReadWriteLock();
		long _expiryTime;

		MyContext(String val) {
			this(val, System.currentTimeMillis()+(30*1000));
		}
		
		MyContext(String val, long expiryTime) {
			_val=val;
			_expiryTime=expiryTime;
		}
		
		MyContext() {}
		
		public Sync getSharedLock(){return _lock.readLock();}
		public Sync getExclusiveLock(){return _lock.writeLock();}

		// Motable...
		
		public long getExpiryTime(){return _expiryTime;}
		
		// SerializableContext...
		
		public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
			_val=(String)oi.readObject();
		}
		
		public void writeContent(ObjectOutput oo) throws IOException, ClassNotFoundException {
			oo.writeObject(_val);
		}
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
		Contextualiser disc=new LocalDiscContextualiser(new DummyContextualiser(), collapser, d, new NeverEvicter(), new File("/tmp"), ss, new MyContextPool());
		Map m=new HashMap();
		m.put("foo", new MyContext("foo"));
		Contextualiser memory=new MemoryContextualiser(disc, collapser, m, new NeverEvicter(), new MyContextPool());
		
		FilterChain fc=new MyFilterChain();
//		Collapser collapser=new HashingCollapser();
		memory.contextualise(null,null,fc,"foo", null, null);
		memory.contextualise(null,null,fc,"bar", null, null);
		assertTrue(!f.exists());
	}
	
	class MyPromotingContextualiser implements Contextualiser {
		int _counter=0;
		MyContext _context;

		public MyPromotingContextualiser(String context) {
			_context=new MyContext(context);
		}
		
		public boolean contextualise(ServletRequest req, ServletResponse res, FilterChain chain, String id, Promoter promoter, Sync promotionMutex) throws IOException, ServletException {
			_counter++;
			
			MyContext context=_context;
			assertTrue(_context!=null);
			if (promoter.prepare(id, context)) {
				_context=null;
				promoter.commit(id, context);
				promotionMutex.release();
				promoter.contextualise(req, res, chain, id, context);
			} else {
				_context=context;
				promoter.rollback(id, context);
			}
			return true;
		}
		
		public void demote(String key, Motable val){}
		public void evict(){}
	}
	
	class MyActiveContextualiser implements Contextualiser {
		int _counter=0;
		MyContext _context;

		public MyActiveContextualiser(String context) {
			_context=new MyContext(context);
		}
		
		public boolean contextualise(ServletRequest req, ServletResponse res, FilterChain chain, String id, Promoter promoter, Sync promotionMutex) throws IOException, ServletException {
			_counter++;
			Context context=_context;
			Sync shared=context.getSharedLock();
			try {
				shared.acquire();
				promotionMutex.release();
				_log.info("running locally: "+id);
				chain.doFilter(req, res);
				shared.release();
				return true;
			} catch (InterruptedException e) {
				throw new ServletException("problem processing request for: "+id, e);
			}
		}

		public void demote(String key, Motable val){}
		public void evict(){}
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
				_contextualiser.contextualise(null, null, _chain, _id, null, null);
			} catch (Exception e) {
				_log.error("unexpected problem", e);
				assertTrue(false);
			}
		}
	}
	
	public void testPromotion(Contextualiser c, int n) throws Exception {
		Contextualiser mc=new MemoryContextualiser(c, new HashingCollapser(10, 2000), new HashMap(), new NeverEvicter(), new MyContextPool());
		FilterChain fc=new MyFilterChain();		
		
		for (int i=0; i<n; i++)
			mc.contextualise(null,null,fc,"baz", null, null);
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
		Contextualiser mc=new MemoryContextualiser(c, new HashingCollapser(10, 2000), new HashMap(), new NeverEvicter(), new MyContextPool());
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
		Contextualiser disc=new LocalDiscContextualiser(new DummyContextualiser(), collapser, d, new NeverEvicter(), new File("/tmp"), ss, new MyContextPool());
		Map m=new HashMap();
		m.put("foo", new MyContext("foo"));
		Contextualiser memory=new MemoryContextualiser(disc, collapser, m, new AlwaysEvicter(), new MyContextPool());
		
		FilterChain fc=new MyFilterChain();

		assertTrue(!d.containsKey("foo"));
		assertTrue(m.containsKey("foo"));
		memory.evict(); // should move foo to disc
		assertTrue(d.containsKey("foo"));
		assertTrue(!m.containsKey("foo"));
		memory.contextualise(null,null,fc,"foo", null, null); // should promote foo back into memory
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
		Contextualiser db=new SharedJDBCContextualiser(new DummyContextualiser(), collapser, new NeverEvicter(), _ds, _table, ss);
		Map d=new HashMap();
		Contextualiser disc=new LocalDiscContextualiser(db, collapser, d, new MyEvicter(0), new File("/tmp"), ss, new MyContextPool());
		Map m=new HashMap();
		m.put("foo", new MyContext("foo", System.currentTimeMillis()+2000)); // times out 2 seconds from now...
		Contextualiser memory=new MemoryContextualiser(disc, collapser, m, new MyEvicter(1000), new MyContextPool());
			
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
		memory.contextualise(null,null,fc,"foo", null, null); // should be promoted to memory
		assertTrue(!d.containsKey("foo"));
		assertTrue(m.containsKey("foo")); // need to be able to 'touch' a context...
		memory.evict(); // should still be there...
//		assertTrue(!d.containsKey("foo"));
//		assertTrue(m.containsKey("foo"));
//		assertTrue(((MyContext)m.get("foo"))._val.equals("foo"));
	}
	
	class MyListener implements MessageListener {
		
		String _id;
		Location _location;
		Cluster _cluster;
		
		MyListener(String id, Location location, Cluster cluster) {
			_id=id;
			_location=location;
			_cluster=cluster;
		}
		
		public void onMessage(Message message) {
			ObjectMessage om=null;
			Object tmp=null;
			String id=null;
			
			try {
				// filter/validate message
				if (message instanceof ObjectMessage &&
					(om=(ObjectMessage)message)!=null &&
					(tmp=om.getObject())!=null &&
					tmp instanceof LocationRequest &&
					(id=((LocationRequest)tmp).getId())!=null &&
					_id.equals(id)) {
					// send a LocationResponse
					_log.info("sending location response: "+_id);
					LocationResponse response=new LocationResponse(_location, Collections.singleton(id));
					ObjectMessage res=_cluster.createObjectMessage();
					res.setJMSReplyTo(message.getJMSReplyTo());
					res.setJMSCorrelationID(message.getJMSCorrelationID());
					res.setObject(response);
					_cluster.send(message.getJMSReplyTo(), res);
					}
			} catch (JMSException e) {
				_log.info("problem sending receiving message: ", e);
			}
		}
	}

	static class MyLocation implements Location, Serializable{
		public long getExpiryTime(){return 0;}
		public void proxy(ServletRequest req, ServletResponse res) {System.out.println("PROXYING!!!");}
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
		
		Collapser collapser0=new HashingCollapser(10, 2000);		
		Map c0=new HashMap();
		c0.put("foo", null); // add a location
		ClusterContextualiser clstr0=new ClusterContextualiser(new DummyContextualiser(), collapser0, c0, new MyEvicter(0), cluster0, 2000);
		Map m0=new HashMap();
		Contextualiser memory0=new MemoryContextualiser(clstr0, collapser0, m0, new NeverEvicter(), new MyContextPool());
		clstr0.setContextualiser(memory0);
		boolean excludeSelf0=true;
	    MessageConsumer consumer0=cluster0.createConsumer(cluster0.getDestination(), null, excludeSelf0);
	    MyLocation location0=new MyLocation();
		MessageListener listener0=new MyListener("foo", location0, cluster0);
	    consumer0.setMessageListener(listener0);
		
		Collapser collapser1=new HashingCollapser(10, 2000);		
		Map c1=new HashMap();
		c1.put("bar", null); // add a location
		ClusterContextualiser clstr1=new ClusterContextualiser(new DummyContextualiser(), collapser1, c1, new MyEvicter(0), cluster1, 2000);
		Map m1=new HashMap();
		Contextualiser memory1=new MemoryContextualiser(clstr1, collapser1, m1, new NeverEvicter(), new MyContextPool());
		clstr1.setContextualiser(memory1);
		boolean excludeSelf1=true;
	    MessageConsumer consumer1=cluster1.createConsumer(cluster1.getDestination(), null, excludeSelf1);
	    MyLocation location1=new MyLocation();
		MessageListener listener1=new MyListener("bar", location1, cluster1);
	    consumer1.setMessageListener(listener1);
		
	    Thread.sleep(2000); // activecluster needs a little time to sort itself out...
	    _log.info("STARTING NOW!");
		FilterChain fc=new MyFilterChain();

		assertTrue(!m0.containsKey("bar"));
		assertTrue(!m1.containsKey("foo"));
		assertTrue(memory0.contextualise(null,null,fc,"bar", null, null));
		assertTrue(memory1.contextualise(null,null,fc,"foo", null, null));
		assertTrue(!memory0.contextualise(null,null,fc,"baz", null, null));
		assertTrue(!memory1.contextualise(null,null,fc,"baz", null, null));
		
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
