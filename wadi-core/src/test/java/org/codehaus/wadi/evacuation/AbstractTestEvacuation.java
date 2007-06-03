package org.codehaus.wadi.evacuation;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.contextualiser.ThrowExceptionIfNoSessionInvocation;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.test.MyStack;
import org.codehaus.wadi.test.TestUtil;

public abstract class AbstractTestEvacuation extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());
	
    private Dispatcher redD;
    private Dispatcher greenD;

	protected void setUp() throws Exception {
        redD = newDispatcher("red");
        redD.start();
        // Implementation note: we really need to wait some time to have a "stable" Dispatcher. For instance, in the
        // case of ActiveCluster, 
        Thread.sleep(1000);

        greenD = newDispatcher("green");
        greenD.start();
        // Implementation note: we really need to wait some time to have a "stable" Dispatcher. For instance, in the
        // case of ActiveCluster, 
        Thread.sleep(1000);
    }

    protected void tearDown() throws Exception {
        redD.stop();
        greenD.stop();
	}
	
    protected abstract Dispatcher newDispatcher(String name) throws Exception;

	public void testEvacuation() throws Exception {
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

        stopRedAndInvokeAgainstGreen(greenD, red, green, name);
        startRedAndInvokeAgainstRed(redD, greenD, red, name);
	}

    private void startRedAndInvokeAgainstRed(Dispatcher redD, Dispatcher greenD, MyStack red, String id) throws Exception {
        red.start();
        TestUtil.waitForDispatcherSeeOthers(new Dispatcher[] { redD, greenD }, 5000);
        boolean success = red.getManager().contextualise(new ThrowExceptionIfNoSessionInvocation(id));
        assertTrue(success);
    }

    private void stopRedAndInvokeAgainstGreen(Dispatcher greenD, MyStack red, MyStack green, String id) throws Exception {
        red.stop();
        TestUtil.waitForDispatcherSeeOthers(new Dispatcher[] { greenD }, 5000);
        boolean success = green.getManager().contextualise(new ThrowExceptionIfNoSessionInvocation(id));
        assertTrue(success);
    }
    
}
