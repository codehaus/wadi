/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.impl.DummyContextualiser;
import org.codehaus.wadi.sandbox.context.impl.HashingCollapser;
import org.codehaus.wadi.sandbox.context.impl.LocalDiscContextualiser;
import org.codehaus.wadi.sandbox.context.impl.MemoryContextualiser;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.NullSync;
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
		Sync   _lock=new NullSync();
		
		MyContext(String val) { 
			_val=val;
		}
		
		public Sync getSharedLock(){return _lock;}
	}
	
	class
	  MyFilterChain
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
		d.put("bar", new MyContext("bar"));
		Contextualiser disc=new LocalDiscContextualiser(new DummyContextualiser(), d);
		Map m=new HashMap();
		m.put("foo", new MyContext("foo"));
		Contextualiser memory=new MemoryContextualiser(disc, m);
		
		FilterChain fc=new MyFilterChain();
//		Collapser collapser=new HashingCollapser();
		memory.contextualise(null,null,fc,"foo", null, new Mutex());
		memory.contextualise(null,null,fc,"bar", null, new Mutex());
	}
	
	class MyPromotingContextualiser implements Contextualiser {
		int _counter=0;
		MyContext _context;

		public MyPromotingContextualiser(String context) {
			_context=new MyContext(context);
		}
		
		public boolean contextualise(ServletRequest req, ServletResponse res, FilterChain chain, String id, Promoter promoter, Sync overlap) throws IOException, ServletException {
			_counter++;
			promoter.promoteAndContextualise(req, res, chain, id, _context, overlap);
			return true;
		}
	}
	
	class MyRunnable implements Runnable {
		Contextualiser _contextualiser;
		FilterChain    _chain;
		String         _id;
		Mutex          _loadMutex;
		
		MyRunnable(Contextualiser contextualiser, FilterChain chain, String id, Mutex loadMutex) {
			_contextualiser=contextualiser;
			_chain=chain;
			_id=id;
			_loadMutex=loadMutex;
		}
		
		public void run() {
			try {
				_contextualiser.contextualise(null, null, _chain, _id, null, _loadMutex);
			} catch (Exception ignore) {
				assertTrue(false);
			}
		}
	}
	
	public void testPromotion() throws Exception {
		MyPromotingContextualiser mpc=new MyPromotingContextualiser("baz");
		Contextualiser c=new MemoryContextualiser(mpc, new HashMap());
		FilterChain fc=new MyFilterChain();		
		Mutex loadMutex=new Mutex();
		
		for (int i=0; i<2; i++)
			c.contextualise(null,null,fc,"baz", null, loadMutex);
		
		assertTrue(mpc._counter==1);
	}
	
	public void testCollapsing() throws Exception {
		MyPromotingContextualiser mpc=new MyPromotingContextualiser("baz");
		Contextualiser c=new MemoryContextualiser(mpc, new HashMap());
		FilterChain fc=new MyFilterChain();
		Mutex loadMutex=new Mutex();

		Runnable r=new MyRunnable(c, fc, "baz", loadMutex);
		Thread[] threads=new Thread[100];
		for (int i=0; i<100; i++)
			(threads[i]=new Thread(r)).start();
		for (int i=0; i<100; i++)
			threads[i].join();
		
		assertTrue(mpc._counter==1);
		}
	
	// same thing as above - but high concurrency to test thread collapsing...
}
