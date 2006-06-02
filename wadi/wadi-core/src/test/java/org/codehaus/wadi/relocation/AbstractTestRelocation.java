package org.codehaus.wadi.relocation;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.axiondb.jdbc.AxionDataSource;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.EndPoint;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.PoolableInvocationWrapper;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.StandardManager;
import org.codehaus.wadi.test.MockInvocation;
import org.codehaus.wadi.test.MyHttpServletRequest;
import org.codehaus.wadi.test.MyHttpServletResponse;
import org.codehaus.wadi.test.MyStack;
import org.codehaus.wadi.web.WebSession;

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
    
    static class MyInvocation implements Invocation {

        protected Log _log = LogFactory.getLog(getClass());
        protected StandardManager _manager;
        protected String _key;
        protected WebSession _session;
        
        public void init(StandardManager manager, String key) {
            _manager=manager;
            _key=key;
        }
        
        public void clear() {
            _key=null;
        }

        public String getSessionKey() {
            return _key;
        }

        public boolean getRelocatable() {
            return true;
        }

        public void setSession(Session session) {
            throw new UnsupportedOperationException("NYI");
        }
        
        public void invoke(PoolableInvocationWrapper wrapper) throws InvocationException {
            throw new UnsupportedOperationException("NYI");
        }

        public void invoke() throws InvocationException {
            assertTrue(_session!=null);
        }

        public boolean isProxiedInvocation() {
            throw new UnsupportedOperationException("NYI");
        }

        public void relocate(EndPoint endPoint) {
            throw new UnsupportedOperationException("NYI - relocate to: "+endPoint);
            // clever stuff here...
        }

        public void sendError(int code, String message) throws InvocationException {
            throw new UnsupportedOperationException("NYI");
        }

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
		while (redD.getCluster().getPeerCount()<2) {
			Thread.sleep(500);
			_log.info("waiting: "+redD.getCluster().getPeerCount());
		}
		_log.info("...DONE");

		_log.info("WAITING FOR GREEN TO SEE RED...");
		while (greenD.getCluster().getPeerCount()<2) {
			Thread.sleep(500);
			_log.info("waiting: "+greenD.getCluster().getPeerCount());
		}
		_log.info("...DONE");

		_log.info("CREATING SESSION...");
		String id=red.getManager().create(null).getId();
		_log.info("...DONE");

		assertTrue(id!=null);

		FilterChain fc=new FilterChain() {
			public void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
			}
		};

		Invocation invocation=new MockInvocation(new MyHttpServletRequest(), new MyHttpServletResponse(), fc);
		_log.info("RELOCATING SESSION...");
		boolean success=green.getManager().contextualise(invocation, id, null, null, false);
		_log.info("...DONE");

		assertTrue(success);

		_log.info("RED STOPPING...");
		red.stop(); // causes exception
		_log.info("...RED STOPPED");

		_log.info("WAITING FOR GREEN TO UNSEE RED...");
		while (greenD.getCluster().getPeerCount()>1) {
			Thread.sleep(500);
			_log.info("waiting: "+greenD.getCluster().getPeerCount());
		}
		_log.info("...DONE");

		_log.info("GREEN STOPPING...");
		green.stop();
		_log.info("...GREEN STOPPED");
	}
}
