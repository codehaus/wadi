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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.sandbox.context.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.context.impl.LocalDiscContextualiser;
import org.codehaus.wadi.sandbox.context.impl.MemoryContextualiser;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
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
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
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
		
		MyContext(String val) { 
			_val=val;
		}
		
		MyContext() {
		}
		
		public Sync getSharedLock(){return _lock.readLock();}
		
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
	
	public void testContextualiser() throws Exception {
		Map d=new HashMap();
		StreamingStrategy ss=new SimpleStreamingStrategy();
		File f=File.createTempFile("wadi.", "."+ss.getSuffix());
		_log.info("file: "+f);
	    ObjectOutput oo=ss.getOutputStream(new FileOutputStream(f));
	    new MyContext("bar").writeContent(oo);
	    oo.flush();
	    oo.close();
	    assertTrue(f.exists());
		d.put("bar", f);
		Contextualiser disc=new LocalDiscContextualiser(new DummyContextualiser(), d, ss);
		Map m=new HashMap();
		m.put("foo", new MyContext("foo"));
		Contextualiser memory=new MemoryContextualiser(disc, m, new MyContextPool());
		
		FilterChain fc=new MyFilterChain();
//		Collapser collapser=new HashingCollapser();
		memory.contextualise(null,null,fc,"foo", null, new Mutex());
		memory.contextualise(null,null,fc,"bar", null, new Mutex());
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
			promoter.promoteAndContextualise(req, res, chain, id, _context, promotionMutex);
			return true;
		}
	}
	
	class MyXContextualiser implements Contextualiser {
		int _counter=0;
		MyContext _context;

		public MyXContextualiser(String context) {
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
	}
	
	class MyRunnable implements Runnable {
		Contextualiser _contextualiser;
		FilterChain    _chain;
		String         _id;
		Mutex          _promotionMutex;
		
		MyRunnable(Contextualiser contextualiser, FilterChain chain, String id, Mutex promotionMutex) {
			_contextualiser=contextualiser;
			_chain=chain;
			_id=id;
			_promotionMutex=promotionMutex;
		}
		
		public void run() {
			try {
				_contextualiser.contextualise(null, null, _chain, _id, null, _promotionMutex);
			} catch (Exception ignore) {
				assertTrue(false);
			}
		}
	}
	
	public void testPromotion(Contextualiser c, int n) throws Exception {
		Contextualiser mc=new MemoryContextualiser(c, new HashMap(), new MyContextPool());
		FilterChain fc=new MyFilterChain();		
		Mutex promotionMutex=new Mutex();
		
		for (int i=0; i<n; i++)
			mc.contextualise(null,null,fc,"baz", null, promotionMutex);
	}
	
	public void testPromotion() throws Exception {
		int n=2;
		MyPromotingContextualiser mpc=new MyPromotingContextualiser("baz");
		testPromotion(mpc, n);	
		assertTrue(mpc._counter==1);
		
		MyXContextualiser mxc=new MyXContextualiser("baz");
		testPromotion(mxc, n);	
		assertTrue(mxc._counter==n);
	}
	
	public void testCollapsing(Contextualiser c, int n) throws Exception {
		Contextualiser mc=new MemoryContextualiser(c, new HashMap(), new MyContextPool());
		FilterChain fc=new MyFilterChain();
		Mutex promotionMutex=new Mutex();

		Runnable r=new MyRunnable(mc, fc, "baz", promotionMutex);
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
		
		MyXContextualiser mxc=new MyXContextualiser("baz");
		testCollapsing(mxc, n);
		assertTrue(mxc._counter==n);
		}
}
