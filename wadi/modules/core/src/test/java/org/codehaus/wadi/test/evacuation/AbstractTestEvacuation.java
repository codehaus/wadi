package org.codehaus.wadi.test.evacuation;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.axiondb.jdbc.AxionDataSource;
import org.codehaus.wadi.InvocationContext;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.WebInvocationContext;
import org.codehaus.wadi.test.MyHttpServletRequest;
import org.codehaus.wadi.test.MyHttpServletResponse;
import org.codehaus.wadi.test.MyStack;

public class AbstractTestEvacuation extends TestCase {
	
	protected Log _log = LogFactory.getLog(getClass());
	protected String _url = "jdbc:axiondb:WADI";
	protected DataSource _ds = new AxionDataSource(_url);
	
	public AbstractTestEvacuation(String arg0) {
		super(arg0);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testEvacuation(Dispatcher red, Dispatcher green) throws Exception {
		MyStack stack1=new MyStack(_url, _ds, red);
		_log.info("RED STARTING...");
		stack1.start();
		_log.info("...RED STARTED");
		MyStack stack2=new MyStack(_url, _ds, green);
		_log.info("GREEN STARTING...");
		stack2.start();
		_log.info("...GREEN STARTED");
		
		_log.info("WAITING FOR RED TO SEE GREEN...");
		while (red.getNumNodes()<2) {
			Thread.sleep(500);
			_log.info("waiting: "+red.getNumNodes());
		}
		_log.info("...DONE");
		
		_log.info("WAITING FOR GREEN TO SEE RED...");
		while (green.getNumNodes()<2) {
			Thread.sleep(500);
			_log.info("waiting: "+green.getNumNodes());
		}
		_log.info("...DONE");
		
		_log.info("CREATING SESSION...");
		String id=stack1.getManager().create().getId();
		_log.info("...DONE");
		
		assertTrue(id!=null);
		
		Thread.sleep(5000);// prevents us stopping whilst a Partition transfer is ongoing....
		
		_log.info("RED STOPPING...");
		stack1.stop();
		_log.info("...RED STOPPED");
		
		boolean success=false;
		try {
			FilterChain fc=new FilterChain() {
				public void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
					HttpSession session=((HttpServletRequest)req).getSession();
					assertTrue(session!=null);
					_log.info("ACQUIRED SESSION: "+session.getId());
				}
			};
			
			InvocationContext invocation=new WebInvocationContext(new MyHttpServletRequest(), new MyHttpServletResponse(), fc);
			assertTrue(stack2.getManager().contextualise(invocation, id, null, null, true));
			success=true;
		} catch (NullPointerException e) {
		}
		
		assertTrue(success);

		_log.info("WAITING FOR GREEN TO UNSEE RED...");
		while (green.getNumNodes()>1) {
			Thread.sleep(500);
			_log.info("waiting: "+green.getNumNodes());
		}
		_log.info("...DONE");
		
		_log.info("GREEN STOPPING...");
		stack2.stop();
		_log.info("...GREEN STOPPED");
	}
	
}
