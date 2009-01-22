package org.codehaus.wadi.core.contextualiser;

import org.codehaus.wadi.core.session.Session;

import com.agical.rmock.extension.junit.RMockTestCase;

public class BasicInvocationTest extends RMockTestCase {

    private Session session;
    private BasicInvocation invocation;
    private InvocationContext invocationContext;

    @Override
    protected void setUp() throws Exception {
        session = (Session) mock(Session.class);
        invocationContext = (InvocationContext) mock(InvocationContext.class);
        
        invocation = new BasicInvocation("key", 1);
        invocation.setSession(session);
    }
    
    public void testExecuteOnEndProcessingWithExistingSession() throws Exception {
        session.onEndProcessing();
        modify().multiplicity(expect.exactly(2));
        
        startVerification();
        
        invocation.invoke();
        invocation.invoke(invocationContext);
    }
    
    public void testExecuteOnEndProcessingWithNewSession() throws Exception {
        session.isNew();
        modify().multiplicity(expect.exactly(2)).returnValue(true);

        session.onEndProcessing();
        modify().multiplicity(expect.exactly(2));

        startVerification();
        
        invocation.setDoNotExecuteOnEndProcessing(true);
        
        invocation.invoke();
        invocation.invoke(invocationContext);
    }
    
    public void testDoNotExecuteOnEndProcessing() throws Exception {
        session.isNew();
        modify().multiplicity(expect.exactly(2)).returnValue(false);

        startVerification();
        
        invocation.setDoNotExecuteOnEndProcessing(true);
        
        invocation.invoke();
        invocation.invoke(invocationContext);
    }
    
    public void testIEThrownByDoInvokeIsRethrown() throws Exception {
        final InvocationException exception = new InvocationException();
        invocation = new BasicInvocation("key", 1) {
            @Override
            protected void doInvoke() throws InvocationException {
                throw exception;
            }
        };
        
        try {
            invocation.invoke();
            fail();
        } catch (InvocationException e) {
            assertSame(exception, e);
        }
    }
    
    public void testIEThrownByDoInvokeWithContextIsRethrown() throws Exception {
        final InvocationException exception = new InvocationException();
        invocation = new BasicInvocation("key", 1) {
            @Override
            protected void doInvoke(InvocationContext context) throws InvocationException {
                throw exception;
            }
        };
        
        try {
            invocation.invoke(new BasicInvocationContext(invocation));
            fail();
        } catch (InvocationException e) {
            assertSame(exception, e);
        }
    }
    
}
