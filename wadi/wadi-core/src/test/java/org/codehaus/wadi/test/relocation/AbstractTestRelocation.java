package org.codehaus.wadi.test.relocation;

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

public class AbstractTestRelocation extends TestCase {

	protected Log _log = LogFactory.getLog(getClass());
	protected String _url = "jdbc:axiondb:WADI";
	protected DataSource _ds = new AxionDataSource(_url);

	public AbstractTestRelocation(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSessionRelocation(Dispatcher redD, Dispatcher greenD) throws Exception {
		MyStack red=new MyStack(_url, _ds, redD);
		_log.info("RED STARTING...");
		red.start();
		_log.info("...RED STARTED");
		MyStack green=new MyStack(_url, _ds, greenD);
		_log.info("GREEN STARTING...");
		green.start();
		_log.info("...GREEN STARTED");

		_log.info("WAITING FOR RED TO SEE GREEN...");
		while (redD.getNumNodes()<2) {
			Thread.sleep(500);
			_log.info("waiting: "+redD.getNumNodes());
		}
		_log.info("...DONE");

		_log.info("WAITING FOR GREEN TO SEE RED...");
		while (greenD.getNumNodes()<2) {
			Thread.sleep(500);
			_log.info("waiting: "+greenD.getNumNodes());
		}
		_log.info("...DONE");

		_log.info("CREATING SESSION...");
		String id=red.getManager().create().getId();
		_log.info("...DONE");

		assertTrue(id!=null);

		//Thread.sleep(5000);// FIXME: prevents us relocating whilst a Partition transfer is ongoing....

		FilterChain fc=new FilterChain() {
			public void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
				HttpSession session=((HttpServletRequest)req).getSession();
				assertTrue(session!=null);
				_log.info("ACQUIRED SESSION: "+session.getId());
			}
		};

		InvocationContext invocation=new WebInvocationContext(new MyHttpServletRequest(), new MyHttpServletResponse(), fc);
		_log.info("RELOCATING SESSION...");
		boolean success=green.getManager().contextualise(invocation, id, null, null, false);
		_log.info("...DONE");

		assertTrue(success);

		_log.info("RED STOPPING...");
		red.stop(); // causes exception
		_log.info("...RED STOPPED");

		_log.info("WAITING FOR GREEN TO UNSEE RED...");
		while (greenD.getNumNodes()>1) {
			Thread.sleep(500);
			_log.info("waiting: "+greenD.getNumNodes());
		}
		_log.info("...DONE");

		_log.info("GREEN STOPPING...");
		green.stop();
		_log.info("...GREEN STOPPED");
	}
}
