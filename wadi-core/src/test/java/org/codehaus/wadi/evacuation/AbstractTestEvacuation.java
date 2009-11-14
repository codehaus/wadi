package org.codehaus.wadi.evacuation;

import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.contextualiser.ThrowExceptionIfNoSessionInvocation;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.test.AbstractTwoNodesTest;
import org.codehaus.wadi.test.TestUtil;

public abstract class AbstractTestEvacuation extends AbstractTwoNodesTest {

    public void testEvacuation() throws Exception {
        Session session = red.getManager().create(null);
        session.onEndProcessing();
        Object id = session.getId();

        stopRedAndInvokeAgainstGreen(greenSSDispatcher, red, green, id);
        startRedAndInvokeAgainstRed(redSSDispatcher, greenD, red, id);
	}

    private void startRedAndInvokeAgainstRed(Dispatcher redD, Dispatcher greenD, StackContext red, Object id) throws Exception {
        red.getServiceSpace().start();
        TestUtil.waitForDispatcherSeeOthers(new Dispatcher[] { redD, greenD }, 5000);
        boolean success = red.getManager().contextualise(new ThrowExceptionIfNoSessionInvocation(id, 2000));
        assertTrue(success);
    }

    private void stopRedAndInvokeAgainstGreen(Dispatcher greenD, StackContext red, StackContext green, Object id) throws Exception {
        red.getServiceSpace().stop();
        TestUtil.waitForDispatcherSeeOthers(new Dispatcher[] { greenD }, 5000);
        boolean success = green.getManager().contextualise(new ThrowExceptionIfNoSessionInvocation(id, 2000));
        assertTrue(success);
    }
    
}
