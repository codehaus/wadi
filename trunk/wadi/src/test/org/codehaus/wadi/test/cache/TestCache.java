/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.cache;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.test.cache.impl.InactiveEvicter;
import org.codehaus.wadi.test.cache.impl.InvalidEvicter;
import org.codehaus.wadi.test.cache.impl.JDBCCache;
import org.codehaus.wadi.test.cache.impl.LocalDiscCache;
import org.codehaus.wadi.test.cache.impl.ClusterCache;
import org.codehaus.wadi.test.cache.impl.MemoryCache;
import org.codehaus.wadi.test.cache.impl.MaxInactiveIntervalEvicter;
import org.codehaus.wadi.test.cache.impl.NeverEvicter;

import junit.framework.TestCase;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestCache extends TestCase {
	protected static Log _log = LogFactory.getLog(TestCache.class);
	
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
	 * Constructor for TestCache.
	 * @param arg0
	 */
	public TestCache(String arg0) {
		super(arg0);
	}

	class MyRequestProcessor implements RequestProcessor {
		String _content;
		
		public MyRequestProcessor(String content) {
			_content=content;
		}
		
		public boolean equals(Object o) {
			if (o instanceof MyRequestProcessor) {
				MyRequestProcessor that=(MyRequestProcessor)o;
				return this._content.equals(that._content);
			}
			else
				return false;
		}
		
		public void process(ServletRequest req, ServletResponse res, FilterChain chain) {
			_log.info("processing MyRequest: "+_content);
		}
		
		protected long _ttl;
		public long getTimeToLive() {return _ttl;}
		public void setTimeToLive(long ttl) {_ttl=ttl;}

		protected int _mii;
		public int getMaxInactiveInterval() {return _mii;}
		public void setMaxInactiveInterval(int mii) {_mii=mii;}
		
		
		public void readContent(ObjectInput is)
		    throws IOException, ClassNotFoundException {
			_content=(String)is.readObject();
		  }

		public void writeContent(ObjectOutput os)
		    throws IOException, ClassNotFoundException {
			os.writeObject(_content);
		  }
		}
	
	
	public void testCacheStack()
	throws Exception
	{
	    // ugly - is there a better way - createTempDir ?
	    File f=File.createTempFile("TestJCache-", "", new File("/tmp"));
	    String name=f.toString();
	    f.delete();
	    File dir=new File(name);
	    _log.info("dir="+dir);
	    assertTrue(dir.mkdirs());
	    
	    // TODO - insert Replicated and DB cache tiers here...
		Cache database=new JDBCCache(new NeverEvicter(), null);
		Cache cluster=new ClusterCache(new InvalidEvicter(), database);
		Cache disc=new LocalDiscCache(dir, new SimpleStreamingStrategy(), new MaxInactiveIntervalEvicter(), cluster);
		Cache cache=new MemoryCache(new InactiveEvicter(), disc);
		
		String key="xxx";
		RequestProcessor val=new MyRequestProcessor("test");
		val.setTimeToLive(0);
		cache.put(key, val);
		cache.evict();
		disc.evict();
		
		RequestProcessor tmp=cache.get(key);
//		
//		assertTrue(val.equals(tmp));
//		
//		tmp.process(null, null, null);
//		
//		String k="foo";
//		RequestProcessor foo=new MyRequestProcessor(k);
//		cluster.put(k, foo);
//		
//		cache.get(k).process(null, null, null);
	}
}
