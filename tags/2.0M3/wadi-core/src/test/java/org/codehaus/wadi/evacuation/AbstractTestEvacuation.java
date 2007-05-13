package org.codehaus.wadi.evacuation;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.test.MockInvocation;
import org.codehaus.wadi.test.MyHttpServletRequest;
import org.codehaus.wadi.test.MyHttpServletResponse;
import org.codehaus.wadi.test.MyStack;
import org.codehaus.wadi.test.TestUtil;
import org.codehaus.wadi.web.HttpInvocationContext;
import org.codehaus.wadi.web.impl.WebInvocation;

public class AbstractTestEvacuation extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());
	
	public AbstractTestEvacuation(String arg0) {
		super(arg0);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testEvacuation(Dispatcher redD, Dispatcher greenD) throws Exception {
		MyStack red = new MyStack(redD);
        red.start();
        redD = red.getServiceSpace().getDispatcher();

        MyStack green = new MyStack(greenD);
        green.start();
        greenD = green.getServiceSpace().getDispatcher();

        TestUtil.waitForDispatcherSeeOthers(new Dispatcher[] { redD, greenD }, 5000);

        Session session = red.getManager().create(null);
        session.onEndProcessing();
        String name = session.getName();

        FilterChain fc = new FilterChain() {
            public void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
                HttpInvocationContext invocationContext = (HttpInvocationContext) req;
                WebInvocation webInvocation = invocationContext.getWebInvocation();
                assertTrue(webInvocation.getSession() != null);
            }
        };

        stopRedAndInvokeAgainstGreen(greenD, red, green, name, fc);
        startRedAndInvokeAgainstRed(redD, greenD, red, name, fc);
	}

    private void startRedAndInvokeAgainstRed(Dispatcher redD, Dispatcher greenD, MyStack red, String id, FilterChain fc) throws Exception, InvocationException {
        red.start();
        TestUtil.waitForDispatcherSeeOthers(new Dispatcher[] { redD, greenD }, 5000);
        Invocation invocation = new MockInvocation(new MyHttpServletRequest(id), new MyHttpServletResponse(), fc);
        boolean success = red.getManager().contextualise(invocation);
        assertTrue(success);
    }

    private void stopRedAndInvokeAgainstGreen(Dispatcher greenD, MyStack red, MyStack green, String id, FilterChain fc) throws Exception, InvocationException {
        red.stop();
        TestUtil.waitForDispatcherSeeOthers(new Dispatcher[] { greenD }, 5000);
        Invocation invocation = new MockInvocation(new MyHttpServletRequest(id), new MyHttpServletResponse(), fc);
        boolean success = green.getManager().contextualise(invocation);
        assertTrue(success);
    }
}