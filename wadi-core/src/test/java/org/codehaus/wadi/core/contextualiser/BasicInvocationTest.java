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
    
    public void testExecuteOnEndProcessing() throws Exception {
        session.onEndProcessing();
        session.onEndProcessing();
        startVerification();
        
        invocation.invoke();
        invocation.invoke(invocationContext);
    }
    
    public void testDoNotExecuteOnEndProcessing() throws Exception {
        startVerification();
        
        invocation.setDoNotExecuteOnEndProcessing(true);
        
        invocation.invoke();
        invocation.invoke(invocationContext);
    }
    

}
